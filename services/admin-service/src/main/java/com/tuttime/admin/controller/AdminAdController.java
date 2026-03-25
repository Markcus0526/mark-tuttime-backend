package com.tuttime.admin.controller;

import com.tuttime.admin.dto.AdRequest;
import com.tuttime.admin.dto.AdResponse;
import com.tuttime.admin.dto.RejectRequest;
import com.tuttime.admin.service.AdminAdService;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Ad Management Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/ads")
@RequiredArgsConstructor
@Tag(name = "Admin Ad Management", description = "Ad management APIs for admins")
public class AdminAdController {

	private final AdminAdService adService;
	private final AdminAuditService auditService;

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List all ads", description = "Get paginated list of all ads with optional status filter")
	public ResponseEntity<ApiResponse<Page<AdResponse>>> listAds(
			@PageableDefault(size = 50) Pageable pageable,
			@RequestParam(required = false) String status,
			@AuthenticationPrincipal Long adminId) {

		Page<AdResponse> ads = adService.listAds(pageable, status);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.AD_VIEWED,
				"AD", null, "Listed all ads");

		return ResponseEntity.ok(ApiResponse.success("Ads retrieved successfully", ads));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get ad details", description = "Get details of a specific ad")
	public ResponseEntity<ApiResponse<AdResponse>> getAd(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		AdResponse ad = adService.getAd(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.AD_VIEWED,
				"AD", id, "Viewed ad details");

		return ResponseEntity.ok(ApiResponse.success("Ad retrieved successfully", ad));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Create ad", description = "Create a new ad")
	public ResponseEntity<ApiResponse<AdResponse>> createAd(
			@Valid @RequestBody AdRequest request,
			@AuthenticationPrincipal Long adminId) {

		AdResponse ad = adService.createAd(request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.AD_CREATED,
				"AD", ad.getAdId(),
				String.format("Created ad: %s", request.getAdTitle()));

		return ResponseEntity.ok(ApiResponse.success("Ad created successfully", ad));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Update ad", description = "Update an existing ad")
	public ResponseEntity<ApiResponse<AdResponse>> updateAd(
			@PathVariable Long id,
			@Valid @RequestBody AdRequest request,
			@AuthenticationPrincipal Long adminId) {

		AdResponse ad = adService.updateAd(id, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.AD_UPDATED,
				"AD", id, "Updated ad");

		return ResponseEntity.ok(ApiResponse.success("Ad updated successfully", ad));
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Delete ad", description = "Delete an ad")
	public ResponseEntity<ApiResponse<String>> deleteAd(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		adService.deleteAd(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.AD_DELETED,
				"AD", id, "Deleted ad");

		return ResponseEntity.ok(ApiResponse.success("Ad deleted successfully"));
	}

	@PostMapping("/{id}/approve")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Approve ad", description = "Approve a pending or draft ad")
	public ResponseEntity<ApiResponse<AdResponse>> approveAd(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		AdResponse ad = adService.approveAd(id, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.AD_APPROVED,
				"AD", id, "Approved ad");

		return ResponseEntity.ok(ApiResponse.success("Ad approved successfully", ad));
	}

	@PostMapping("/{id}/reject")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Reject ad", description = "Reject a pending or draft ad")
	public ResponseEntity<ApiResponse<AdResponse>> rejectAd(
			@PathVariable Long id,
			@Valid @RequestBody RejectRequest request,
			@AuthenticationPrincipal Long adminId) {

		AdResponse ad = adService.rejectAd(id, adminId, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.AD_REJECTED,
				"AD", id, "Rejected ad: " + request.getReason());

		return ResponseEntity.ok(ApiResponse.success("Ad rejected successfully", ad));
	}
}

