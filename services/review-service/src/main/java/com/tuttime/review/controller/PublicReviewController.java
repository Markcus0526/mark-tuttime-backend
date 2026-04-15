package com.tuttime.review.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.review.dto.RatingStatsResponse;
import com.tuttime.review.dto.ReviewRequest;
import com.tuttime.review.dto.ReviewResponse;
import com.tuttime.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "1. Customer Reviews", description = "Public endpoints for posting and viewing reviews")
public class PublicReviewController {

	private final ReviewService reviewService;

	@PostMapping
	@Operation(summary = "Submit a new review", description = "Initializes a review in PENDING status for moderation")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Review submitted successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request data")
	})
	public ResponseEntity<ApiRes<ReviewResponse>> postReview(
			@Valid @RequestBody ReviewRequest request) {
		UUID userId = UUID.fromString(UserContext.getUserId());
		ReviewResponse response = reviewService.createReview(userId, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiRes.success(response));
	}

	@GetMapping("/restaurant/{restaurantId}/stats")
	@Operation(summary = "Get restaurant stats", description = "Returns average rating and star distribution")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Stats retrieved successfully"),
			@ApiResponse(responseCode = "404", description = "Restaurant not found")
	})
	public ResponseEntity<ApiRes<RatingStatsResponse>> getStats(@PathVariable UUID restaurantId) {
		RatingStatsResponse response = reviewService.getRestaurantStats(restaurantId);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	/**
	 * Private helper to keep the controller DRY
	 */
	private void validateLogin() {
		if (!UserContext.isValidUUID()) {
			throw new UnauthorizedException("Unauthorized: You should login");
		}
	}

	private void validateAdmin() {
		if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: You do not manage this method");
		}
	}
}
