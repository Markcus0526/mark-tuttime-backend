package com.tuttime.review.service;

import com.tuttime.review.dto.RatingStatsResponse;
import com.tuttime.review.dto.ReviewRequest;
import com.tuttime.review.dto.ReviewResponse;

import java.util.UUID;

/**
 * Service interface for managing restaurant reviews and aggregate ratings.
 */
public interface ReviewService {

	/**
	 * Creates a new review in PENDING status.
	 */
	ReviewResponse createReview(UUID userId, ReviewRequest request);

	/**
	 * Approves a review and updates the restaurant's aggregate rating.
	 */
	void approveReview(UUID reviewId, UUID moderatorId, String reason);

	/**
	 * Rejects a review for policy violations.
	 */
	void rejectReview(UUID reviewId, UUID moderatorId, String reason);

	/**
	 * Retrieves aggregated ratings for a specific restaurant.
	 */
	RatingStatsResponse getRestaurantStats(UUID restaurantId);
}
