package com.tuttime.admin.controller;

import com.tuttime.admin.dto.AppVersionResponse;
import com.tuttime.admin.dto.UpdateAppVersionRequest;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AppVersionService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Admin App Version Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/app/version")
@RequiredArgsConstructor
@Tag(name = "Admin App Version", description = "App version management APIs")
public class AdminAppVersionController {

	private final AppVersionService appVersionService;
	private final AdminAuditService auditService;

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'VIEWER')")
	@Operation(summary = "Get app version", description = "Get current app version and install links for Android and iOS")
	public ResponseEntity<ApiResponse<AppVersionResponse>> getAppVersion(
			@AuthenticationPrincipal Long adminId) {

		log.debug("Getting app version - requested by admin: {}", adminId);
		AppVersionResponse version = appVersionService.getAppVersion();
		return ResponseEntity.ok(ApiResponse.success("App version retrieved successfully", version));
	}

	@PutMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update app version", description = "Update app version numbers and install links (SUPER_ADMIN/ADMIN only)")
	public ResponseEntity<ApiResponse<AppVersionResponse>> updateAppVersion(
			@Valid @RequestBody UpdateAppVersionRequest request,
			@AuthenticationPrincipal Long adminId) {

		log.info("Updating app version - requested by admin: {}", adminId);

		try {
			AppVersionResponse version = appVersionService.updateAppVersion(request, adminId);

			auditService.logAction(adminId, null,
					com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
					"APP_VERSION", null,
					String.format("Updated app version - Android: %s, iOS: %s",
							request.getAndroidVersion(), request.getIosVersion()));

			return ResponseEntity.ok(ApiResponse.success("App version updated successfully", version));
		} catch (com.tuttime.common.exception.BusinessException e) {
			log.warn("Business exception while updating app version: {}", e.getMessage());
			// Return the business exception message directly for frontend display
			return ResponseEntity.badRequest()
					.body(ApiResponse.error(e.getMessage()));
		} catch (Exception e) {
			log.error("Unexpected error while updating app version", e);
			return ResponseEntity.internalServerError()
					.body(ApiResponse.error("An unexpected error occurred while updating app version. Please try again."));
		}
	}
}

