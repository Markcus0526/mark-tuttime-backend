package com.tuttime.review.service;

import com.tuttime.review.dto.ReviewDTO;
import com.tuttime.review.dto.ReviewRequest;
import com.tuttime.review.dto.ReviewResponse;
import com.tuttime.review.entity.Review;
import com.tuttime.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import java.util.List;
import java.util.UUID;

public interface ReviewService {

	ReviewDTO getReviewById(UUID id);

	List<ReviewDTO> getReviewsByRestaurant(UUID restaurantId);

	List<ReviewDTO> getReviewsByUser(UUID userId);

	List<ReviewDTO> getReviewsByStatus(String status);

	ReviewDTO createReview(ReviewDTO reviewDTO);

	ReviewDTO updateReview(UUID id, ReviewDTO reviewDTO);

	void deleteReview(UUID id);
}