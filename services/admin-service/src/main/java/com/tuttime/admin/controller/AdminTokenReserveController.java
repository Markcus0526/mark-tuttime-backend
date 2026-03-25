package com.tuttime.admin.controller;

import com.tuttime.admin.dto.ReserveAlarmRequest;
import com.tuttime.admin.dto.ReserveAlarmResponse;
import com.tuttime.admin.dto.TokenReserveResponse;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminTokenReserveService;
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

import java.util.List;

/**
 * Admin Token Reserve Observation Controller
 *
 * <p>Provides endpoints for monitoring token reserves:
 * <ul>
 *   <li>View token reserves for each network (Treasury, HotWallet balances)</li>
 *   <li>View token price, total supply, NAV</li>
 *   <li>Set up alarms for low balance, price drops, etc.</li>
 *   <li>View active alarms</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/observations/token-reserves")
@RequiredArgsConstructor
@Tag(name = "Admin Token Reserve Observation", description = "Token reserves monitoring and alarm management APIs")
public class AdminTokenReserveController {

	private final AdminTokenReserveService tokenReserveService;
	private final AdminAuditService auditService;

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get reserves for all networks",
			description = "Retrieves aggregated token reserves across all blockchain networks. " +
					"TODO: Integrate with blockchain contracts to fetch real-time data.")
	public ResponseEntity<ApiResponse<com.tuttime.admin.dto.TokenReservesAggregatedResponse>> getAllReserves(
			@AuthenticationPrincipal Long adminId) {

		// TODO: Integrate with blockchain contracts to fetch real-time reserves
		// - Connect to smart contracts for each network (Ethereum, BSC, Polygon, etc.)
		// - Fetch treasury balance from contract
		// - Fetch hot wallet balance from contract
		// - Fetch token price from DEX or price oracle
		// - Fetch total supply from contract
		// - Calculate NAV = totalSupply * tokenPrice
		// - Aggregate balances across all networks

		com.tuttime.admin.dto.TokenReservesAggregatedResponse reserves = tokenReserveService.getAggregatedReserves();
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_RESERVE_VIEWED,
				"TOKEN_RESERVE", null, "Viewed aggregated token reserves");

		return ResponseEntity.ok(ApiResponse.success("Token reserves retrieved successfully", reserves));
	}

	@GetMapping("/{network}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get reserves for specific network",
			description = "Retrieves token reserves for a specific blockchain network")
	public ResponseEntity<ApiResponse<TokenReserveResponse>> getReserveByNetwork(
			@PathVariable String network,
			@AuthenticationPrincipal Long adminId) {

		TokenReserveResponse reserve = tokenReserveService.getReserveByNetwork(network);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_RESERVE_VIEWED,
				"TOKEN_RESERVE", null, "Viewed token reserve for network: " + network);

		return ResponseEntity.ok(ApiResponse.success("Token reserve retrieved successfully", reserve));
	}

	@GetMapping("/alarms")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get active alarms",
			description = "Retrieves all active token reserve alarms. " +
					"TODO: Integrate alarm checking/triggering with scheduled jobs.")
	public ResponseEntity<ApiResponse<List<ReserveAlarmResponse>>> getActiveAlarms(
			@RequestParam(required = false) String network,
			@AuthenticationPrincipal Long adminId) {

		// TODO: Integrate alarm checking/triggering
		// 1. Set up scheduled job (@Scheduled) to check alarms periodically (e.g., every 5 minutes)
		// 2. In scheduled job, call checkAndTriggerAlarms() method
		// 3. For each active alarm:
		//    - Fetch current reserve value based on metricType (treasuryBalance, hotWalletBalance, tokenPrice, totalSupply, nav)
		//    - Compare with threshold using comparisonOperator
		//    - If condition met, trigger alarm:
		//      * Update alarm status to TRIGGERED
		//      * Set triggeredAt timestamp
		//      * Send notification (email, Slack, webhook, etc.)
		//    - If condition no longer met and alarm was triggered, mark as resolved
		// 4. Integrate with notification service for alerting

		List<ReserveAlarmResponse> alarms;
		if (network != null && !network.isEmpty()) {
			alarms = tokenReserveService.getAlarmsByNetwork(network);
		} else {
			alarms = tokenReserveService.getActiveAlarms();
		}

		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_RESERVE_VIEWED,
				"TOKEN_RESERVE_ALARM", null, "Viewed token reserve alarms");

		return ResponseEntity.ok(ApiResponse.success("Alarms retrieved successfully", alarms));
	}

	@PostMapping("/alarms")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Create alarm threshold",
			description = "Creates a new alarm threshold for token reserve monitoring. " +
					"TODO: Integrate alarm checking/triggering with scheduled jobs.")
	public ResponseEntity<ApiResponse<ReserveAlarmResponse>> createAlarm(
			@Valid @RequestBody ReserveAlarmRequest request,
			@AuthenticationPrincipal Long adminId) {

		// TODO: Validate alarm configuration
		// - If alarmType is LOW_BALANCE, ensure threshold is reasonable
		// - If alarmType is PRICE_DROP/PRICE_INCREASE, ensure threshold is percentage or absolute value
		// - If network is specified, validate network exists in token_reserves table

		ReserveAlarmResponse alarm = tokenReserveService.createAlarm(request, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_RESERVE_ALARM_CREATED,
				"TOKEN_RESERVE_ALARM", alarm.getAlarmId(),
				"Created alarm: type=" + request.getAlarmType() + ", network=" + request.getNetwork());

		return ResponseEntity.ok(ApiResponse.success("Alarm created successfully", alarm));
	}

	@PutMapping("/alarms/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update alarm",
			description = "Updates an existing token reserve alarm")
	public ResponseEntity<ApiResponse<ReserveAlarmResponse>> updateAlarm(
			@PathVariable Long id,
			@Valid @RequestBody ReserveAlarmRequest request,
			@AuthenticationPrincipal Long adminId) {

		ReserveAlarmResponse alarm = tokenReserveService.updateAlarm(id, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_RESERVE_ALARM_UPDATED,
				"TOKEN_RESERVE_ALARM", id, "Updated alarm: type=" + request.getAlarmType());

		return ResponseEntity.ok(ApiResponse.success("Alarm updated successfully", alarm));
	}

	@DeleteMapping("/alarms/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Delete alarm",
			description = "Deletes a token reserve alarm")
	public ResponseEntity<ApiResponse<Void>> deleteAlarm(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		tokenReserveService.deleteAlarm(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_RESERVE_ALARM_DELETED,
				"TOKEN_RESERVE_ALARM", id, "Deleted alarm");

		return ResponseEntity.ok(ApiResponse.success("Alarm deleted successfully", null));
	}
}

