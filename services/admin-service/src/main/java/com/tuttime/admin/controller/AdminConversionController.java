package com.tuttime.admin.controller;

import com.tuttime.admin.dto.ConversionInfoResponse;
import com.tuttime.admin.dto.ConversionResponse;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminConversionService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin Conversion Management Controller
 * Returns static values when conversion-service is unavailable
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/conversions")
@RequiredArgsConstructor
@Tag(name = "Admin Conversion Management", description = "AP to APART conversion management APIs")
public class AdminConversionController {

	private final AdminConversionService conversionService;
	private final AdminAuditService auditService;

	@GetMapping("/users/{userId}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get user conversions", description = "Get conversion history for a user")
	public ResponseEntity<ApiResponse<Page<ConversionResponse>>> getUserConversions(
			@PathVariable Long userId,
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<ConversionResponse> conversions = conversionService.getUserConversions(userId, pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONVERSION_VIEWED,
				"CONVERSION", userId, "Viewed user conversions");

		return ResponseEntity.ok(ApiResponse.success("Conversions retrieved successfully", conversions));
	}

	@GetMapping("/users/{userId}/info")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get conversion info", description = "Get conversion information and forecast for a user")
	public ResponseEntity<ApiResponse<ConversionInfoResponse>> getConversionInfo(
			@PathVariable Long userId,
			@AuthenticationPrincipal Long adminId) {

		ConversionInfoResponse info = conversionService.getConversionInfo(userId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONVERSION_VIEWED,
				"CONVERSION", userId, "Viewed conversion info");

		return ResponseEntity.ok(ApiResponse.success("Conversion info retrieved successfully", info));
	}

	@GetMapping("/monthly/{year}/{month}/ledger")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get monthly conversion ledger", description = "Get full ledger for monthly conversion with user details")
	public ResponseEntity<ApiResponse<Page<com.tuttime.admin.dto.ConversionLedgerEntry>>> getMonthlyConversionLedger(
			@PathVariable int year,
			@PathVariable int month,
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<com.tuttime.admin.dto.ConversionLedgerEntry> ledger = conversionService.getMonthlyConversionLedger(year, month, pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONVERSION_VIEWED,
				"CONVERSION", null, String.format("Viewed monthly conversion ledger for %d/%d", year, month));

		return ResponseEntity.ok(ApiResponse.success("Monthly conversion ledger retrieved successfully", ledger));
	}
}

