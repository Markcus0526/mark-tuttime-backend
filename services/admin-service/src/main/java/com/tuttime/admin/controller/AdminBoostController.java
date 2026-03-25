package com.tuttime.admin.controller;

import com.tuttime.admin.dto.BookingBoostResponse;
import com.tuttime.admin.dto.RentalBoostResponse;
import com.tuttime.admin.dto.VideoBoostResponse;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminBoostService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Boost View Controller
 * Provides read-only endpoints for viewing booking and rental boosts
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/boosts")
@RequiredArgsConstructor
@Tag(name = "Admin Boost View", description = "Admin APIs for viewing boosts")
public class AdminBoostController {

	private final AdminBoostService boostService;
	private final AdminAuditService auditService;

	@GetMapping("/bookings")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List all booking boosts", description = "Get paginated list of all booking boosts with optional user filter")
	public ResponseEntity<ApiResponse<Page<BookingBoostResponse>>> listBookingBoosts(
			@PageableDefault(size = 50) Pageable pageable,
			@RequestParam(required = false) Long userId,
			@AuthenticationPrincipal Long adminId) {

		Page<BookingBoostResponse> boosts = boostService.listBookingBoosts(pageable, userId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BOOST_VIEWED,
				"BOOST", null, "Listed all booking boosts" + (userId != null ? " for user: " + userId : ""));

		return ResponseEntity.ok(ApiResponse.success("Booking boosts retrieved successfully", boosts));
	}

	@GetMapping("/bookings/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get booking boost details", description = "Get details of a specific booking boost")
	public ResponseEntity<ApiResponse<BookingBoostResponse>> getBookingBoost(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		BookingBoostResponse boost = boostService.getBookingBoost(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BOOST_VIEWED,
				"BOOST", id, "Viewed booking boost details");

		return ResponseEntity.ok(ApiResponse.success("Booking boost retrieved successfully", boost));
	}

	@GetMapping("/rentals")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List all rental boosts", description = "Get paginated list of all rental boosts with optional user filter")
	public ResponseEntity<ApiResponse<Page<RentalBoostResponse>>> listRentalBoosts(
			@PageableDefault(size = 50) Pageable pageable,
			@RequestParam(required = false) Long userId,
			@AuthenticationPrincipal Long adminId) {

		Page<RentalBoostResponse> boosts = boostService.listRentalBoosts(pageable, userId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BOOST_VIEWED,
				"BOOST", null, "Listed all rental boosts" + (userId != null ? " for user: " + userId : ""));

		return ResponseEntity.ok(ApiResponse.success("Rental boosts retrieved successfully", boosts));
	}

	@GetMapping("/rentals/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get rental boost details", description = "Get details of a specific rental boost")
	public ResponseEntity<ApiResponse<RentalBoostResponse>> getRentalBoost(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		RentalBoostResponse boost = boostService.getRentalBoost(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BOOST_VIEWED,
				"BOOST", id, "Viewed rental boost details");

		return ResponseEntity.ok(ApiResponse.success("Rental boost retrieved successfully", boost));
	}

	@GetMapping("/videos")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List all video boosts", description = "Get paginated list of all video boosts with optional user filter")
	public ResponseEntity<ApiResponse<Page<VideoBoostResponse>>> listVideoBoosts(
			@PageableDefault(size = 50) Pageable pageable,
			@RequestParam(required = false) Long userId,
			@AuthenticationPrincipal Long adminId) {

		Page<VideoBoostResponse> boosts = boostService.listVideoBoosts(pageable, userId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BOOST_VIEWED,
				"BOOST", null, "Listed all video boosts" + (userId != null ? " for user: " + userId : ""));

		return ResponseEntity.ok(ApiResponse.success("Video boosts retrieved successfully", boosts));
	}

	@GetMapping("/videos/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get video boost details", description = "Get details of a specific video boost")
	public ResponseEntity<ApiResponse<VideoBoostResponse>> getVideoBoost(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		VideoBoostResponse boost = boostService.getVideoBoost(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BOOST_VIEWED,
				"BOOST", id, "Viewed video boost details");

		return ResponseEntity.ok(ApiResponse.success("Video boost retrieved successfully", boost));
	}

	@GetMapping("/users/{userId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get all boosts for user", description = "Get all boosts (booking, rental, and video) for a specific user")
	public ResponseEntity<ApiResponse<List<Object>>> getBoostsByUser(
			@PathVariable Long userId,
			@AuthenticationPrincipal Long adminId) {

		List<Object> boosts = boostService.getBoostsByUserId(userId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.BOOST_VIEWED,
				"BOOST", userId, String.format("Viewed boosts for user: %d", userId));

		return ResponseEntity.ok(ApiResponse.success("User boosts retrieved successfully", boosts));
	}
}

