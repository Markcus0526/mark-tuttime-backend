package com.tuttime.review.service.impl;

import com.tuttime.common.enums.ReviewStatus;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.event.ReviewEvent;
import com.tuttime.common.event.proto.GetUserDetailResponse;
import com.tuttime.common.event.proto.GetUserDetailsRequest;
import com.tuttime.common.event.proto.GetUserDetailsResponse;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.review.dto.RatingDto;
import com.tuttime.review.dto.ReviewDto;
import com.tuttime.review.entity.RatingAggregate;
import com.tuttime.review.entity.Review;
import com.tuttime.review.entity.ReviewModerationLog;
import com.tuttime.review.grpc.client.RestaurantGrpcClient;
import com.tuttime.review.grpc.client.UserGrpcClient;
import com.tuttime.review.mapper.ReviewMapper;
import com.tuttime.review.repository.ModerationLogRepository;
import com.tuttime.review.repository.RatingAggregateRepository;
import com.tuttime.review.repository.ReviewRepository;
import com.tuttime.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final RatingAggregateRepository aggregateRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final ReviewMapper reviewMapper;
    private final RestaurantGrpcClient restaurantGrpcClient;
    private final UserGrpcClient userGrpcClient;
    private final EventProducer eventProducer;

    @Override
    @Transactional
    public ReviewDto.ReviewResponse createReview(UUID userId, ReviewDto.ReviewRequest request) {
        restaurantGrpcClient.validateBranch(request.branchId());

        if (reviewRepository.existsByUserIdAndBranchId(userId, request.branchId())) {
            throw new ConflictException("You have already reviewed this branch");
        }

        Review review = Review.builder()
                .userId(userId)
                .branchId(request.branchId())
                .rating(request.rating())
                .comment(request.comment())
                .status(ReviewStatus.PENDING)
                .build();

        Review saved = reviewRepository.save(review);
        dispatchReviewUpdatedEvent(saved);
        return reviewMapper.toReviewResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDto.ReviewResponse> getReviewsByStatus(UUID branchId, ReviewStatus status, Pageable pageable) {
        log.debug("Fetching reviews for branch {} with status {}", branchId, status);

        // 1. Fetch reviews from DB
        Page<Review> reviewPage = reviewRepository.findByBranchIdAndStatus(branchId, status, pageable);
        if (reviewPage.isEmpty()) return Page.empty();

        // 2. Batch fetch user info via gRPC to avoid N+1

        List<String> userIds = reviewPage.getContent().stream()
                .map(review -> review.getUserId().toString())
                .distinct()
                .toList();
        // Call User Service (Assuming a Batch RPC exists)
        GetUserDetailsResponse usersResponse = userGrpcClient.getUserDetails(GetUserDetailsRequest.newBuilder()
                .addAllUserIds(userIds)
                .build());

        Map<String, GetUserDetailResponse> userMap = usersResponse.getProfilesList().stream()
                .collect(Collectors.toMap(GetUserDetailResponse::getUserId, user -> user));

        // 3. Map to DTOs and enrich with User info
        return reviewPage.map(review -> {
            GetUserDetailResponse user = userMap.getOrDefault(
                    review.getUserId().toString(),
                    GetUserDetailResponse.newBuilder().setFullName("Unknown User").build()
            );
            return reviewMapper.toReviewDetailResponse(review, user);
        });
    }

    @Override
    @Transactional
    public ReviewDto.ReviewResponse moderateReview(UUID reviewId, ReviewDto.ReviewModerationRequest request,
                                                   UUID currentUserId, String userRoles) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new InvalidRequestException("Review not found"));

        // Branch permission check
        restaurantGrpcClient.validateBranchPermission(review.getBranchId(), currentUserId, userRoles);

        ReviewStatus oldStatus = review.getStatus();
        if (oldStatus == request.status()) {
            return reviewMapper.toReviewResponse(review);
        }

        review.setStatus(request.status());

        // Log with oldStatus
        ReviewModerationLog logEntry = ReviewModerationLog.builder()
                .review(review)
                .moderatorId(currentUserId)
                .action(request.action())
                .reason(request.reason())
                .build();
        moderationLogRepository.save(logEntry);

        // Update aggregate statistics
        if (request.status() == ReviewStatus.APPROVED && oldStatus != ReviewStatus.APPROVED) {
            updateRatingAggregate(review.getBranchId(), review.getRating());
        } else if (oldStatus == ReviewStatus.APPROVED && request.status() != ReviewStatus.APPROVED) {
            subtractFromRatingAggregate(review.getBranchId(), review.getRating());
        }

        Review updated = reviewRepository.save(review);
        dispatchReviewUpdatedEvent(updated);
        return reviewMapper.toReviewResponse(updated);
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

    private void subtractFromRatingAggregate(UUID branchId, Integer rating) {
        RatingAggregate aggregate = aggregateRepository.findById(branchId)
                .orElseThrow(() -> new InvalidRequestException("No aggregate found for branch"));
        // Update distribution (decrement)
        Map<String, Integer> dist = aggregate.getRatingDistribution();
        String ratingKey = String.valueOf(rating);
        dist.put(ratingKey, dist.getOrDefault(ratingKey, 0) - 1);
        // Update total and average
        int newTotal = aggregate.getTotalReviews() - 1;
        if (newTotal == 0) {
            aggregate.setAverageRating(BigDecimal.ZERO);
        } else {
            BigDecimal totalScore = aggregate.getAverageRating()
                    .multiply(BigDecimal.valueOf(aggregate.getTotalReviews()))
                    .subtract(BigDecimal.valueOf(rating));
            aggregate.setAverageRating(totalScore.divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP));
        }
        aggregate.setTotalReviews(newTotal);
        aggregateRepository.save(aggregate);
    }

    private void dispatchReviewUpdatedEvent(Review review) {
        ReviewEvent.ReviewUpdatedEvent event = ReviewEvent.ReviewUpdatedEvent.builder()
                .reviewId(review.getId())
                .branchId(review.getBranchId())
                .userId(review.getUserId())
                .rating(review.getRating())
                .status(review.getStatus())
                .build();
        eventProducer.sendReviewUpdatedEvent(event);
    }
}
