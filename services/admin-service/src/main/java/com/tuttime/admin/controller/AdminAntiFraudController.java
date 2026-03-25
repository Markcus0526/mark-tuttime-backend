package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminAntiFraudService;
import com.tuttime.admin.service.AdminAuditService;
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

/**
 * Admin Anti-Fraud Management Controller
 *
 * <p>Provides endpoints for managing fraud detection and prevention:
 * <ul>
 *   <li>View fraud flags</li>
 *   <li>Block/unblock devices</li>
 *   <li>Block/unblock IP addresses</li>
 *   <li>View fraud analytics</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/antifraud")
@RequiredArgsConstructor
@Tag(name = "Admin Anti-Fraud Management", description = "Anti-fraud management APIs")
public class AdminAntiFraudController {

	private final AdminAntiFraudService antiFraudService;
	private final AdminAuditService auditService;

	@GetMapping("/flags")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get fraud flags", description = "Get paginated list of fraud flags with optional filters")
	public ResponseEntity<ApiResponse<Page<FraudFlagResponse>>> listFraudFlags(
			@PageableDefault(size = 50) Pageable pageable,
			@RequestParam(required = false) Long userId,
			@RequestParam(required = false) WarningCategory category,
			@RequestParam(required = false) WarningSeverity severity,
			@RequestParam(required = false) Boolean resolved,
			@AuthenticationPrincipal Long adminId) {

		Page<FraudFlagResponse> flags = antiFraudService.listFraudFlags(pageable, userId, category, severity, resolved);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
				"ANTIFRAUD", null, "Listed fraud flags");

		return ResponseEntity.ok(ApiResponse.success("Fraud flags retrieved successfully", flags));
	}

	@PostMapping("/devices/{deviceId}/block")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Block device", description = "Block a device from accessing the system")
	public ResponseEntity<ApiResponse<Void>> blockDevice(
			@PathVariable String deviceId,
			@RequestBody(required = false) BlockDeviceRequest request,
			@AuthenticationPrincipal Long adminId) {

		antiFraudService.blockDevice(deviceId, request != null ? request.getReason() : null);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.DEVICE_BLOCKED,
				"ANTIFRAUD", null, "Blocked device: " + deviceId + ", reason: " + (request != null ? request.getReason() : "No reason"));

		return ResponseEntity.ok(ApiResponse.success("Device blocked successfully", null));
	}

	@PostMapping("/devices/{deviceId}/unblock")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Unblock device", description = "Unblock a device")
	public ResponseEntity<ApiResponse<Void>> unblockDevice(
			@PathVariable String deviceId,
			@AuthenticationPrincipal Long adminId) {

		antiFraudService.unblockDevice(deviceId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_REMOVED,
				"ANTIFRAUD", null, "Unblocked device: " + deviceId);

		return ResponseEntity.ok(ApiResponse.success("Device unblocked successfully", null));
	}

	@PostMapping("/ips/{ipAddress}/block")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Block IP address", description = "Block an IP address from accessing the system. Also blocks all devices with this IP address.")
	public ResponseEntity<ApiResponse<Void>> blockIpAddress(
			@PathVariable String ipAddress,
			@RequestParam(required = false) String reason,
			@AuthenticationPrincipal Long adminId) {

		antiFraudService.blockIpAddress(ipAddress, reason);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.IP_BLOCKED,
				"ANTIFRAUD", null, "Blocked IP: " + ipAddress + ", reason: " + (reason != null ? reason : "No reason"));

		return ResponseEntity.ok(ApiResponse.success("IP address blocked successfully", null));
	}

	@PostMapping("/ips/{ipAddress}/unblock")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
	@Operation(summary = "Unblock IP address", description = "Unblock an IP address")
	public ResponseEntity<ApiResponse<Void>> unblockIpAddress(
			@PathVariable String ipAddress,
			@AuthenticationPrincipal Long adminId) {

		antiFraudService.unblockIpAddress(ipAddress);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_REMOVED,
				"ANTIFRAUD", null, "Unblocked IP: " + ipAddress);

		return ResponseEntity.ok(ApiResponse.success("IP address unblocked successfully", null));
	}

	@GetMapping("/analytics")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get fraud analytics", description = "Get fraud detection analytics and statistics")
	public ResponseEntity<ApiResponse<FraudAnalyticsResponse>> getFraudAnalytics(
			@AuthenticationPrincipal Long adminId) {

		FraudAnalyticsResponse analytics = antiFraudService.getFraudAnalytics();
		return ResponseEntity.ok(ApiResponse.success("Fraud analytics retrieved successfully", analytics));
	}

	@GetMapping("/devices/blocked")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List blocked devices", description = "Get paginated list of all blocked devices")
	public ResponseEntity<ApiResponse<Page<BlockedDeviceResponse>>> listBlockedDevices(
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<BlockedDeviceResponse> devices = antiFraudService.listBlockedDevices(pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
				"ANTIFRAUD", null, "Listed blocked devices");

		return ResponseEntity.ok(ApiResponse.success("Blocked devices retrieved successfully", devices));
	}

	@GetMapping("/ips/blocked")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List blocked IPs", description = "Get paginated list of all blocked IP addresses")
	public ResponseEntity<ApiResponse<Page<BlockedIpResponse>>> listBlockedIps(
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<BlockedIpResponse> ips = antiFraudService.listBlockedIps(pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
				"ANTIFRAUD", null, "Listed blocked IPs");

		return ResponseEntity.ok(ApiResponse.success("Blocked IPs retrieved successfully", ips));
	}

	@GetMapping("/devices/whitelisted")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List whitelisted devices", description = "Get paginated list of all whitelisted devices")
	public ResponseEntity<ApiResponse<Page<WhitelistedDeviceResponse>>> listWhitelistedDevices(
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<WhitelistedDeviceResponse> devices = antiFraudService.listWhitelistedDevices(pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
				"ANTIFRAUD", null, "Listed whitelisted devices");

		return ResponseEntity.ok(ApiResponse.success("Whitelisted devices retrieved successfully", devices));
	}

	@GetMapping("/ips/whitelisted")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List whitelisted IPs", description = "Get paginated list of all whitelisted IP addresses")
	public ResponseEntity<ApiResponse<Page<WhitelistedIpResponse>>> listWhitelistedIps(
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<WhitelistedIpResponse> ips = antiFraudService.listWhitelistedIps(pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
				"ANTIFRAUD", null, "Listed whitelisted IPs");

		return ResponseEntity.ok(ApiResponse.success("Whitelisted IPs retrieved successfully", ips));
	}

	@PostMapping("/devices/{deviceId}/whitelist")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Add device to whitelist", description = "Add a device to the whitelist")
	public ResponseEntity<ApiResponse<Void>> whitelistDevice(
			@PathVariable String deviceId,
			@RequestBody(required = false) BlockDeviceRequest request,
			@AuthenticationPrincipal Long adminId) {

		antiFraudService.whitelistDevice(deviceId, request != null ? request.getReason() : null);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.DEVICE_WHITELISTED,
				"ANTIFRAUD", null, "Whitelisted device: " + deviceId);

		return ResponseEntity.ok(ApiResponse.success("Device whitelisted successfully", null));
	}

	@PostMapping("/ips/{ipAddress}/whitelist")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Add IP to whitelist", description = "Add an IP address to the whitelist")
	public ResponseEntity<ApiResponse<Void>> whitelistIpAddress(
			@PathVariable String ipAddress,
			@RequestBody(required = false) BlockIpRequest request,
			@AuthenticationPrincipal Long adminId) {

		antiFraudService.whitelistIpAddress(ipAddress, request != null ? request.getReason() : null);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.IP_WHITELISTED,
				"ANTIFRAUD", null, "Whitelisted IP: " + ipAddress);

		return ResponseEntity.ok(ApiResponse.success("IP address whitelisted successfully", null));
	}
}

