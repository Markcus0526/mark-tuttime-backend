package com.tuttime.review.controller;

import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.review.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@Tag(name = "2. Review Moderation", description = "Internal endpoints for managing review approvals")
public class AdminReviewController {

	private final ReviewService reviewService;

	@PatchMapping("/{reviewId}/approve")
	@Operation(summary = "Approve review", description = "Updates status to APPROVED and triggers rating recalculation")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Review approved successfully"),
			@ApiResponse(responseCode = "404", description = "Review not found")
	})
	public ResponseEntity<ApiRes<Void>> approve(
			@PathVariable UUID reviewId,
			@RequestParam(defaultValue = "Policy compliant") String reason) {
		UUID moderatorId = UUID.fromString(UserContext.getUserId());
		reviewService.approveReview(reviewId, moderatorId, reason);
		return ResponseEntity.ok(ApiRes.success(null));
	}

	@PatchMapping("/{reviewId}/reject")
	@Operation(summary = "Reject review", description = "Sets review to REJECTED status with a reason")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Review rejected successfully"),
			@ApiResponse(responseCode = "400", description = "Missing rejection reason")
	})
	public ResponseEntity<ApiRes<Void>> reject(
			@PathVariable UUID reviewId,
			@RequestParam String reason) {
		UUID moderatorId = UUID.fromString(UserContext.getUserId());
		reviewService.rejectReview(reviewId, moderatorId, reason);
		return ResponseEntity.ok(ApiRes.success(null));
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
