package com.tuttime.review.service.impl;

import com.tuttime.common.event.ReviewEvent;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.review.dto.*;
import com.tuttime.review.entity.*;
import com.tuttime.review.entity.enums.ReviewStatus;
import com.tuttime.review.grpc.client.RestaurantGrpcClient;
import com.tuttime.review.kafka.ReviewEventProducer;
import com.tuttime.review.mapper.ReviewMapper;
import com.tuttime.review.repository.*;
import com.tuttime.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;
	private final RatingAggregateRepository aggregateRepository;
	private final ModerationLogRepository moderationLogRepository;
	private final ReviewMapper reviewMapper;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final ReviewEventProducer reviewEventProducer;

	@Override
	@Transactional
	public ReviewDto.ReviewResponse createReview(UUID userId, ReviewDto.ReviewRequest request) {
		restaurantGrpcClient.validateBranch(request.branchId());

		Review review = Review.builder()
				.userId(userId)
				.branchId(request.branchId())
				.rating(request.rating())
				.comment(request.comment())
				.status(ReviewStatus.PENDING)
				.build();

		return reviewMapper.toReviewResponse(reviewRepository.save(review));
	}

	@Override
	@Transactional(readOnly = true)
	public Page<ReviewDto.ReviewResponse> getReviewsByStatus(UUID branchId, ReviewStatus status, Pageable pageable) {
		log.debug("Fetching reviews for branch {} with status {}", branchId, status);

		// 1. Fetch from repository (Hits idx_reviews_restaurant_status)
		Page<Review> reviewPage = reviewRepository.findByBranchIdAndStatus(branchId, status, pageable);

		// 2. Map to DTOs using MapStruct
		return reviewPage.map(reviewMapper::toReviewResponse);
	}

	@Override
	@Transactional
	public ReviewDto.ReviewResponse moderateReview(UUID reviewId, UUID moderatorId, ReviewDto.ReviewModerationRequest request) {
		// 1. Fetch Review (Optimistic locking version is automatically checked here)
		Review review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new InvalidRequestException("Review not found with ID: " + reviewId));

		// 2. Business Rule: Prevent redundant processing
		ReviewStatus oldStatus = review.getStatus();
		if (oldStatus == request.status()) {
			return reviewMapper.toReviewResponse(review);
		}

		// 3. Update Review Status
		review.setStatus(request.status());

		// 4. Persistence: Create Moderation Log entry
		ReviewModerationLog logEntry = ReviewModerationLog.builder()
				.review(review)
				.moderatorId(moderatorId)
				.action(request.action())
				.reason(request.reason())
				.build();
		moderationLogRepository.save(logEntry);

		// 5. Aggregate logic: Only update stats if the review was JUST approved
		if (request.status() == ReviewStatus.APPROVED) {
			updateRatingAggregate(review.getBranchId(), review.getRating());
		}

		// 6. If a review was APPROVED and is now being REJECTED/DELETED,
		// you should ideally subtract it from aggregates (Logic omitted for brevity)

		// Save and return mapped response
		Review updatedReview = reviewRepository.save(review);
		log.info("Review {} moderated to {} by moderator {}", reviewId, request.status(), moderatorId);

		if (updatedReview.getStatus() == ReviewStatus.APPROVED) {
			ReviewEvent.ReviewApprovedEvent event = new ReviewEvent.ReviewApprovedEvent(
					UUID.randomUUID(),
					review.getId(),
					review.getBranchId(),
					review.getUserId(),
					review.getRating(),
					OffsetDateTime.now()
			);
			reviewEventProducer.sendReviewApprovedEvent(event);
		}

		return reviewMapper.toReviewResponse(updatedReview);
	}

	@Override
	@Transactional(readOnly = true)
	public RatingDto.RatingAggregateResponse getAggregateStats(UUID branchId) {
		return aggregateRepository.findById(branchId)
				.map(reviewMapper::toRatingResponse) // Map the entity if found
				.orElseGet(() -> RatingDto.RatingAggregateResponse.builder()
						.branchId(branchId)
						.averageRating(BigDecimal.ZERO)
						.totalReviews(0)
						.ratingDistribution(Map.of("1", 0, "2", 0, "3", 0, "4", 0, "5", 0))
						.build());
	}

	private void updateRatingAggregate(UUID branchId, Integer newRating) {
		RatingAggregate aggregate = aggregateRepository.findById(branchId)
				.orElse(RatingAggregate.builder()
						.branchId(branchId)
						.averageRating(BigDecimal.ZERO)
						.totalReviews(0)
						.ratingDistribution(new java.util.HashMap<>(Map.of("1", 0, "2", 0, "3", 0, "4", 0, "5", 0)))
						.build());

		// Update distribution map
		Map<String, Integer> dist = aggregate.getRatingDistribution();
		String ratingKey = String.valueOf(newRating);
		dist.put(ratingKey, dist.getOrDefault(ratingKey, 0) + 1);

		// Update total and average
		int newTotal = aggregate.getTotalReviews() + 1;
		BigDecimal totalScore = aggregate.getAverageRating()
				.multiply(BigDecimal.valueOf(aggregate.getTotalReviews()))
				.add(BigDecimal.valueOf(newRating));

		aggregate.setTotalReviews(newTotal);
		aggregate.setAverageRating(totalScore.divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP));

		aggregateRepository.save(aggregate);
	}
}
