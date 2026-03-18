package com.tuttime.reviewservice.controller;

import com.tuttime.reviewservice.dto.ReviewRequest;
import com.tuttime.reviewservice.dto.ReviewResponse;
import com.tuttime.reviewservice.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

  private final ReviewService reviewService;

  @PostMapping
  public ResponseEntity<ReviewResponse> createReview(@RequestBody ReviewRequest request) {
    return ResponseEntity.ok(reviewService.createReview(request));
  }

  @GetMapping("/restaurants/{restaurantId}")
  public ResponseEntity<List<ReviewResponse>> getReviewsByRestaurant(@PathVariable Long restaurantId) {
    return ResponseEntity.ok(reviewService.getReviewsByRestaurant(restaurantId));
  }

}
