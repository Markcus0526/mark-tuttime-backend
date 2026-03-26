package com.tuttime.review.controller;

import com.tuttime.review.dto.ReviewDTO;
import com.tuttime.review.dto.ReviewRequest;
import com.tuttime.review.dto.ReviewResponse;
import com.tuttime.review.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@Validated
@Tag(name = "Review Service", description = "Operations related to user reviews")
public class ReviewController {

	private final ReviewService reviewService;

	public ReviewController(ReviewService reviewService) {
		this.reviewService = reviewService;
	}

	@Operation(summary = "Get review by ID", responses = {
			@ApiResponse(responseCode = "200", description = "Review found"),
			@ApiResponse(responseCode = "404", description = "Review not found")
	})
	@GetMapping("/{id}")
	public ResponseEntity<ReviewDTO> getReviewById(
			@Parameter(description = "UUID of the review to retrieve", required = true)
			@PathVariable @NotNull UUID id) {
		ReviewDTO dto = reviewService.getReviewById(id);
		return ResponseEntity.ok(dto);
	}

	@Operation(summary = "Get reviews by restaurant ID")
	@GetMapping("/restaurant/{restaurantId}")
	public ResponseEntity<List<ReviewDTO>> getReviewsByRestaurant(
			@Parameter(description = "UUID of the restaurant", required = true)
			@PathVariable @NotNull UUID restaurantId) {
		List<ReviewDTO> reviews = reviewService.getReviewsByRestaurant(restaurantId);
		return ResponseEntity.ok(reviews);
	}

	@Operation(summary = "Get reviews by user ID")
	@GetMapping("/user/{userId}")
	public ResponseEntity<List<ReviewDTO>> getReviewsByUser(
			@Parameter(description = "UUID of the user", required = true)
			@PathVariable @NotNull UUID userId) {
		List<ReviewDTO> reviews = reviewService.getReviewsByUser(userId);
		return ResponseEntity.ok(reviews);
	}

	@Operation(summary = "Get reviews by status")
	@GetMapping("/status/{status}")
	public ResponseEntity<List<ReviewDTO>> getReviewsByStatus(
			@Parameter(description = "Status of the review (PENDING, APPROVED, REJECTED, FLAGGED)", required = true)
			@PathVariable
			@NotNull
			@Pattern(regexp = "PENDING|APPROVED|REJECTED|FLAGGED", message = "Invalid status value")
			String status) {
		List<ReviewDTO> reviews = reviewService.getReviewsByStatus(status);
		return ResponseEntity.ok(reviews);
	}

	@Operation(summary = "Create a new review")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewDTO createReview(
			@Parameter(description = "Review data", required = true)
			@Valid @RequestBody ReviewDTO reviewDTO) {
		return reviewService.createReview(reviewDTO);
	}

	@Operation(summary = "Update an existing review")
	@PutMapping("/{id}")
	public ResponseEntity<ReviewDTO> updateReview(
			@Parameter(description = "UUID of the review to update", required = true)
			@PathVariable @NotNull UUID id,
			@Parameter(description = "Updated review data", required = true)
			@Valid @RequestBody ReviewDTO reviewDTO) {
		ReviewDTO updated = reviewService.updateReview(id, reviewDTO);
		return ResponseEntity.ok(updated);
	}

	@Operation(summary = "Delete a review by ID")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteReview(
			@Parameter(description = "UUID of the review to delete", required = true)
			@PathVariable @NotNull UUID id) {
		reviewService.deleteReview(id);
	}
}
