package com.tuttime.review.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.review.dto.RatingDto;
import com.tuttime.review.dto.ReviewDto;
import com.tuttime.review.entity.enums.ReviewStatus;
import com.tuttime.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
@Tag(name = "Customer Reviews and Moderation", description = "Public endpoints for posting and viewing reviews")
public class ReviewController {

	private final ReviewService reviewService;

	@PostMapping
	@Operation(summary = "Submit a new review", description = "Initializes a review in PENDING status for moderation")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Review submitted successfully"),
			@ApiResponse(responseCode = "401", description = "Unauthorized - Login required")
	})
	public ResponseEntity<ApiRes<ReviewDto.ReviewResponse>> postReview(@Valid @RequestBody ReviewDto.ReviewRequest request) {

		validateLogin(); // Clean validation call

		UUID userId = UUID.fromString(UserContext.getUserId());
		return ResponseEntity.status(HttpStatus.CREATED)
				.body(ApiRes.success(reviewService.createReview(userId, request)));
	}

	@GetMapping("/branch/{branchId}")
	@Operation(summary = "Get reviews by branch", description = "Retrieves reviews for a branch. Defaults to APPROVED.")
	public ResponseEntity<ApiRes<Page<ReviewDto.ReviewResponse>>> getReviewsByBranch(
			@PathVariable UUID branchId,
			@Parameter(description = "Filter by status (PENDING, APPROVED, etc.)")
			@RequestParam(defaultValue = "APPROVED") ReviewStatus status,
			@RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size,
			@RequestParam(defaultValue = "createdAt") String sort,
			@RequestParam(defaultValue = "desc") String direction) {

		// Security: Only staff can see non-approved reviews
		if (status != ReviewStatus.APPROVED) {
			validateAdminOrOwnerOrManager();
		}

		// Convert individual params into a PageRequest for the service
		Sort sortOrder = direction.equalsIgnoreCase("asc")
				? Sort.by(sort).ascending()
				: Sort.by(sort).descending();
		Pageable pageable = PageRequest.of(page, size, sortOrder);

		return ResponseEntity.ok(ApiRes.success(reviewService.getReviewsByStatus(branchId, status, pageable)));
	}

	@GetMapping("/branch/{branchId}/stats")
	@Operation(summary = "Get branch rating stats", description = "Returns average rating and star distribution map")
	public ResponseEntity<ApiRes<RatingDto.RatingAggregateResponse>> getBranchStats(@PathVariable UUID branchId) {
		return ResponseEntity.ok(ApiRes.success(reviewService.getAggregateStats(branchId)));
	}

	@PatchMapping("/moderate/{reviewId}")
	@Operation(summary = "Moderate a review", description = "Updates review status and logs action. Requires ADMIN/OWNER/MANAGER role.")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Review moderated successfully"),
			@ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions")
	})
	public ResponseEntity<ApiRes<ReviewDto.ReviewResponse>> moderateReview(
			@PathVariable UUID reviewId,
			@Valid @RequestBody ReviewDto.ReviewModerationRequest request) {

		validateAdminOrOwnerOrManager(); // Centralized security check

		UUID moderatorId = UUID.fromString(UserContext.getUserId());
		ReviewDto.ReviewResponse response = reviewService.moderateReview(reviewId, moderatorId, request);
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
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwner() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}

	private void validateAdminOrOwnerOrManager() {
		if (!UserContext.isValidUUID()
				|| !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
			throw new ForbiddenException("Access forbidden: Insufficient permissions");
		}
	}
}
