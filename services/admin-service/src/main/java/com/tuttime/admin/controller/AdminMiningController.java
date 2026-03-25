package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminMiningFormulaService;
import com.tuttime.admin.service.AdminMiningService;
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
 * Admin Mining Management Controller
 *
 * <p>Provides endpoints for managing AP Credits and mining:
 * <ul>
 *   <li>View AP balances</li>
 *   <li>View mining sessions</li>
 *   <li>View mining speed</li>
 *   <li>Manage mining formula configuration (now stored in system_config table)</li>
 * </ul>
 *
 * <p>Note: Mining formula configuration is stored in the system_config table
 * (previously in mining_formula_config which has been merged).
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/mining")
@RequiredArgsConstructor
@Tag(name = "Admin Mining Management", description = "AP Credits and mining management APIs")
public class AdminMiningController {

	private final AdminMiningService miningService;
	private final AdminMiningFormulaService miningFormulaService;
	private final AdminAuditService auditService;

	@GetMapping("/balances")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List AP balances", description = "Get paginated list of user AP balances")
	public ResponseEntity<ApiResponse<Page<ApBalanceResponse>>> listApBalances(
			@RequestParam(required = false) String search,
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<ApBalanceResponse> balances = miningService.listApBalances(search, pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.MINING_SESSION_VIEWED,
				"MINING", null, "Listed AP balances");

		return ResponseEntity.ok(ApiResponse.success("AP balances retrieved successfully", balances));
	}

	@GetMapping("/users/{userId}/balance")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get user AP balance", description = "Get AP balance for a specific user")
	public ResponseEntity<ApiResponse<ApBalanceResponse>> getUserApBalance(
			@PathVariable Long userId,
			@AuthenticationPrincipal Long adminId) {

		ApBalanceResponse balance = miningService.getUserApBalance(userId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.MINING_SESSION_VIEWED,
				"MINING", userId, "Viewed AP balance");

		return ResponseEntity.ok(ApiResponse.success("AP balance retrieved successfully", balance));
	}

	@GetMapping("/users/{userId}/sessions")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get mining sessions", description = "Get mining session history for a user")
	public ResponseEntity<ApiResponse<Page<MiningSessionResponse>>> getMiningSessions(
			@PathVariable Long userId,
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<MiningSessionResponse> sessions = miningService.getMiningSessions(userId, pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.MINING_SESSION_VIEWED,
				"MINING", userId, "Viewed mining sessions");

		return ResponseEntity.ok(ApiResponse.success("Mining sessions retrieved successfully", sessions));
	}

	@GetMapping("/users/{userId}/speed")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get mining speed", description = "Get current mining speed breakdown for a user")
	public ResponseEntity<ApiResponse<MiningSpeedResponse>> getMiningSpeed(
			@PathVariable Long userId,
			@AuthenticationPrincipal Long adminId) {

		MiningSpeedResponse speed = miningService.getMiningSpeed(userId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.MINING_SESSION_VIEWED,
				"MINING", userId, "Viewed mining speed");

		return ResponseEntity.ok(ApiResponse.success("Mining speed retrieved successfully", speed));
	}

	@GetMapping("/users/{userId}/status")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get mining status", description = "Get comprehensive mining status including balance, speed breakdown, and session info")
	public ResponseEntity<ApiResponse<MiningStatusResponse>> getMiningStatus(
			@PathVariable Long userId,
			@AuthenticationPrincipal Long adminId) {

		MiningStatusResponse status = miningService.getMiningStatus(userId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.MINING_SESSION_VIEWED,
				"MINING", userId, "Viewed mining status");

		return ResponseEntity.ok(ApiResponse.success("Mining status retrieved successfully", status));
	}

	// ==================== Mining Formula Configuration ====================

	@GetMapping("/formula/config")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get mining formula configuration", description = "Get current mining formula parameters")
	public ResponseEntity<ApiResponse<MiningFormulaConfigResponse>> getMiningFormulaConfig(
			@AuthenticationPrincipal Long adminId) {

		MiningFormulaConfigResponse config = miningFormulaService.getMiningFormulaConfig();
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
				"MINING_FORMULA", null, "Viewed mining formula configuration");

		return ResponseEntity.ok(ApiResponse.success("Mining formula configuration retrieved successfully", config));
	}

	@PutMapping("/formula/config")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update mining formula configuration", description = "Update mining formula parameters (SUPER_ADMIN, ADMIN only)")
	public ResponseEntity<ApiResponse<MiningFormulaConfigResponse>> updateMiningFormulaConfig(
			@Valid @RequestBody MiningFormulaConfigRequest request,
			@AuthenticationPrincipal Long adminId) {

		MiningFormulaConfigResponse config = miningFormulaService.updateMiningFormulaConfig(request, adminId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
				"MINING_FORMULA", null, "Updated mining formula configuration");

		return ResponseEntity.ok(ApiResponse.success("Mining formula configuration updated successfully", config));
	}
}

