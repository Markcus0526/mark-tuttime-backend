package com.tuttime.admin.controller;

import com.tuttime.admin.dto.AuditLogResponse;
import com.tuttime.admin.dto.BruteForceHistoryResponse;
import com.tuttime.admin.dto.SystemStabilityHistoryResponse;
import com.tuttime.admin.entity.AdminAuditLog;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminBruteForceHistoryService;
import com.tuttime.admin.service.AdminSystemStabilityHistoryService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Admin Observation Controller
 *
 * <p>Provides endpoints for viewing historical data:
 * <ul>
 *   <li>Brute-force history</li>
 *   <li>System stability history</li>
 *   <li>Audit logs</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/observations")
@RequiredArgsConstructor
@Tag(name = "Admin Observations", description = "Historical observation and audit log APIs")
public class AdminObservationController {

	private final AdminBruteForceHistoryService bruteForceHistoryService;
	private final AdminSystemStabilityHistoryService systemStabilityHistoryService;
	private final AdminAuditService auditService;

	@GetMapping("/brute-force/history")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(
			summary = "Get brute-force history",
			description = "Get paginated list of historical brute-force attack attempts"
	)
	public ResponseEntity<ApiResponse<Page<BruteForceHistoryResponse>>> getBruteForceHistory(
			@PageableDefault(size = 50) Pageable pageable,
			@Parameter(description = "Start date filter (ISO format)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
			@Parameter(description = "End date filter (ISO format)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
			@Parameter(description = "IP address filter")
			@RequestParam(required = false) String ipAddress,
			@Parameter(description = "Phone number filter")
			@RequestParam(required = false) String phoneNumber,
			@AuthenticationPrincipal Long adminId) {

		Page<BruteForceHistoryResponse> history = bruteForceHistoryService.getBruteForceHistory(
				pageable, startDate, endDate, ipAddress, phoneNumber);

		auditService.logAction(adminId, null,
				AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
				"OBSERVATIONS", null, "Viewed brute-force history");

		return ResponseEntity.ok(ApiResponse.success("Brute-force history retrieved successfully", history));
	}

	@GetMapping("/system-stability/history")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(
			summary = "Get system stability history",
			description = "Get paginated list of system stability events and warnings"
	)
	public ResponseEntity<ApiResponse<Page<SystemStabilityHistoryResponse>>> getSystemStabilityHistory(
			@PageableDefault(size = 50) Pageable pageable,
			@Parameter(description = "Start date filter (ISO format)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
			@Parameter(description = "End date filter (ISO format)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
			@Parameter(description = "Event type filter (e.g., ERROR_RATE, RESPONSE_TIME, MEMORY_USAGE)")
			@RequestParam(required = false) String eventType,
			@Parameter(description = "Service name filter (e.g., user-service, token-service)")
			@RequestParam(required = false) String serviceName,
			@AuthenticationPrincipal Long adminId) {

		Page<SystemStabilityHistoryResponse> history = systemStabilityHistoryService.getSystemStabilityHistory(
				pageable, startDate, endDate, eventType, serviceName);

		auditService.logAction(adminId, null,
				AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
				"OBSERVATIONS", null, "Viewed system stability history");

		return ResponseEntity.ok(ApiResponse.success("System stability history retrieved successfully", history));
	}

	@GetMapping("/audit-logs")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(
			summary = "Get admin audit logs",
			description = "Get paginated list of admin action audit logs with optional filters"
	)
	public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
			@PageableDefault(size = 50) Pageable pageable,
			@Parameter(description = "Admin ID filter")
			@RequestParam(required = false) Long adminId,
			@Parameter(description = "Action type filter")
			@RequestParam(required = false) AdminAuditLog.AdminActionType actionType,
			@Parameter(description = "Resource type filter (e.g., USER, TOKEN, MINING)")
			@RequestParam(required = false) String resourceType,
			@Parameter(description = "Start date filter (ISO format)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
			@Parameter(description = "End date filter (ISO format)")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
			@AuthenticationPrincipal Long requestingAdminId) {

		Page<AuditLogResponse> logs = auditService.getAuditLogs(
				adminId, actionType, resourceType, startDate, endDate, pageable);

		auditService.logAction(requestingAdminId, null,
				AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
				"AUDIT_LOGS", null, "Viewed audit logs");

		return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved successfully", logs));
	}
}

