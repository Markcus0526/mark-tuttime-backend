package com.tuttime.review.service.impl;

import com.tuttime.common.event.ReviewEvent;
import com.tuttime.review.dto.RatingStatsResponse;
import com.tuttime.review.dto.ReviewRequest;
import com.tuttime.review.dto.ReviewResponse;
import com.tuttime.review.entity.RatingAggregate;
import com.tuttime.review.entity.Review;
import com.tuttime.review.entity.ReviewModerationLog;
import com.tuttime.review.entity.enums.ReviewStatus;
import com.tuttime.review.kafka.RatingEventProducer;
import com.tuttime.review.repository.ModerationLogRepository;
import com.tuttime.review.repository.RatingAggregateRepository;
import com.tuttime.review.repository.ReviewRepository;
import com.tuttime.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;
	private final RatingAggregateRepository aggregateRepository;
	private final ModerationLogRepository moderationLogRepository;
	private final RatingEventProducer ratingEventProducer;

	@Override
	@Transactional
	public ReviewResponse createReview(UUID userId, ReviewRequest request) {
		log.info("Creating new review for restaurant: {} by user: {}", request.restaurantId(), userId);

		Review review = Review.builder()
				.userId(userId)
				.restaurantId(request.restaurantId())
				.rating(request.rating())
				.comment(request.comment())
				.status(ReviewStatus.PENDING)
				.build();

		Review saved = reviewRepository.save(review);
		return mapToResponse(saved);
	}

	@Override
	@Transactional
	public void approveReview(UUID reviewId, UUID moderatorId, String reason) {
		Review review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new RuntimeException("Review not found with ID: " + reviewId));

		review.setStatus(ReviewStatus.APPROVED);

		// Save Moderation History
		moderationLogRepository.save(ReviewModerationLog.builder()
				.review(review)
				.moderatorId(moderatorId)
				.action("APPROVE")
				.reason(reason)
				.build());

		// Recalculate Aggregate Stats (Page 9 of PDF)
		updateRestaurantAggregate(review.getRestaurantId());

		// 3. Async broadcast via Kafka after aggregate update
		ReviewEvent.ReviewApprovedEvent event = new ReviewEvent.ReviewApprovedEvent(
				review.getId(),
				review.getRestaurantId(),
				review.getUserId(),
				review.getRating(),
				review.getComment(),
				OffsetDateTime.now().toString()
		);
		ratingEventProducer.sendReviewApproved(event);
	}

	@Override
	@Transactional
	public void rejectReview(UUID reviewId, UUID moderatorId, String reason) {
		Review review = reviewRepository.findById(reviewId)
				.orElseThrow(() -> new RuntimeException("Review not found"));

		review.setStatus(ReviewStatus.REJECTED);

		moderationLogRepository.save(ReviewModerationLog.builder()
				.review(review)
				.moderatorId(moderatorId)
				.action("REJECT")
				.reason(reason)
				.build());
	}

	@Override
	@Transactional(readOnly = true)
	public RatingStatsResponse getRestaurantStats(UUID restaurantId) {
		RatingAggregate aggregate = aggregateRepository.findById(restaurantId)
				.orElse(new RatingAggregate(restaurantId, BigDecimal.ZERO, 0, Map.of(), OffsetDateTime.now()));

		return new RatingStatsResponse(
				aggregate.getAverageRating(),
				aggregate.getTotalReviews(),
				aggregate.getRatingDistribution()
		);
	}

	/**
	 * Internal logic to sync the 'rating_aggregates' table.
	 * Uses the JSONB distribution logic from your SQL schema.
	 */
	private void updateRestaurantAggregate(UUID restaurantId) {
		List<Review> approved = reviewRepository.findByRestaurantIdAndStatus(restaurantId, ReviewStatus.APPROVED);

		if (approved.isEmpty()) return;

		double sum = 0;
		Map<String, Integer> dist = new HashMap<>(Map.of("1", 0, "2", 0, "3", 0, "4", 0, "5", 0));

		for (Review r : approved) {
			sum += r.getRating();
			String starKey = String.valueOf(r.getRating());
			dist.put(starKey, dist.getOrDefault(starKey, 0) + 1);
		}

		BigDecimal avg = BigDecimal.valueOf(sum / approved.size()).setScale(2, RoundingMode.HALF_UP);

		RatingAggregate aggregate = aggregateRepository.findById(restaurantId)
				.orElse(new RatingAggregate());

		aggregate.setRestaurantId(restaurantId);
		aggregate.setAverageRating(avg);
		aggregate.setTotalReviews(approved.size());
		aggregate.setRatingDistribution(dist);
		aggregate.setLastUpdated(OffsetDateTime.now());

		aggregateRepository.save(aggregate);
	}

	private ReviewResponse mapToResponse(Review r) {
		return new ReviewResponse(
				r.getId(),
				r.getUserId(),
				r.getRating(),
				r.getComment(),
				r.getStatus().name(),
				r.getCreatedAt().toString()
		);
	}
}
