package com.tuttime.reviewservice.service;

import com.tuttime.reviewservice.dto.ReviewRequest;
import com.tuttime.reviewservice.dto.ReviewResponse;
import com.tuttime.reviewservice.entity.Review;
import com.tuttime.reviewservice.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;

  public ReviewResponse createReview(ReviewRequest request) {
    Review review = Review.builder()
        .userId(request.getUserId())
        .restaurantId(request.getRestaurantId())
        .rating(request.getRating())
        .comment(request.getComment())
        .build();
    review = reviewRepository.save(review);
    return mapToResponse(review);
  }

  public List<ReviewResponse> getReviewsByRestaurant(Long restaurantId) {
    return reviewRepository.findByRestaurantId(restaurantId)
        .stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  private ReviewResponse mapToResponse(Review review) {
    return ReviewResponse.builder()
        .id(review.getId())
        .userId(review.getUserId())
        .restaurantId(review.getRestaurantId())
        .rating(review.getRating().doubleValue())
        .comment(review.getComment())
        .build();
  }
}
