package com.tuttime.review.service;

import com.tuttime.review.dto.*;
import com.tuttime.review.entity.enums.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface ReviewService {
	ReviewDto.ReviewResponse createReview(UUID userId, ReviewDto.ReviewRequest request);

	Page<ReviewDto.ReviewResponse> getReviewsByStatus(UUID branchId, ReviewStatus status, Pageable pageable);

	ReviewDto.ReviewResponse moderateReview(UUID reviewId, UUID moderatorId, ReviewDto.ReviewModerationRequest request);

	RatingDto.RatingAggregateResponse getAggregateStats(UUID branchId);
}
