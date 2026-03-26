package com.tuttime.review.service;

import com.tuttime.review.dto.ReviewDto;

import java.util.List;

import java.util.UUID;

public interface ReviewService {

	ReviewDto getReviewById(UUID id);

	List<ReviewDto> getReviewsByRestaurant(UUID restaurantId);

	List<ReviewDto> getReviewsByUser(UUID userId);

	List<ReviewDto> getReviewsByStatus(String status);

	ReviewDto createReview(ReviewDto reviewDto);

	ReviewDto updateReview(UUID id, ReviewDto reviewDto);

	void deleteReview(UUID id);
}