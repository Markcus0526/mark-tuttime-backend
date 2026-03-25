package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminVestingService;
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
 * Admin Vesting Pool Management Controller
 *
 * <p>Provides endpoints for managing vesting pools and investors.
 * Note: EVM contract integration is pending (see service implementation).
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/vesting/pools")
@RequiredArgsConstructor
@Tag(name = "Admin Vesting Pool Management", description = "Vesting pool and investor management APIs for admins")
public class AdminVestingController {

	private final AdminVestingService vestingService;
	private final AdminAuditService auditService;

	@GetMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List all vesting pools", description = "Get paginated list of all vesting pools")
	public ResponseEntity<ApiResponse<Page<VestingPoolResponse>>> listPools(
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<VestingPoolResponse> pools = vestingService.listPools(pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_POOL_VIEWED,
				"VESTING_POOL", null, "Listed all vesting pools");

		return ResponseEntity.ok(ApiResponse.success("Vesting pools retrieved successfully", pools));
	}

	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get vesting pool details", description = "Get details of a specific vesting pool")
	public ResponseEntity<ApiResponse<VestingPoolResponse>> getPool(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		VestingPoolResponse pool = vestingService.getPool(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_POOL_VIEWED,
				"VESTING_POOL", id, "Viewed vesting pool details");

		return ResponseEntity.ok(ApiResponse.success("Vesting pool retrieved successfully", pool));
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Create vesting pool", description = "Create a new vesting pool")
	public ResponseEntity<ApiResponse<VestingPoolResponse>> createPool(
			@Valid @RequestBody VestingPoolRequest request,
			@AuthenticationPrincipal Long adminId) {

		VestingPoolResponse pool = vestingService.createPool(request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_POOL_CREATED,
				"VESTING_POOL", pool.getPoolId(),
				String.format("Created vesting pool: %s", request.getPoolName()));

		return ResponseEntity.ok(ApiResponse.success("Vesting pool created successfully", pool));
	}

	@PutMapping("/{id}")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Update vesting pool", description = "Update an existing vesting pool")
	public ResponseEntity<ApiResponse<VestingPoolResponse>> updatePool(
			@PathVariable Long id,
			@Valid @RequestBody VestingPoolRequest request,
			@AuthenticationPrincipal Long adminId) {

		VestingPoolResponse pool = vestingService.updatePool(id, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_POOL_UPDATED,
				"VESTING_POOL", id, "Updated vesting pool");

		return ResponseEntity.ok(ApiResponse.success("Vesting pool updated successfully", pool));
	}

	@GetMapping("/{id}/balance")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get pool balance", description = "Get current token balance of a vesting pool (queries EVM contract)")
	public ResponseEntity<ApiResponse<VestingPoolResponse>> getPoolBalance(
			@PathVariable Long id,
			@AuthenticationPrincipal Long adminId) {

		VestingPoolResponse pool = vestingService.getPoolBalance(id);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_POOL_BALANCE_VIEWED,
				"VESTING_POOL", id, "Viewed pool balance");

		return ResponseEntity.ok(ApiResponse.success("Pool balance retrieved successfully", pool));
	}

	@PostMapping("/{id}/fund")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Fund pool", description = "Add tokens to a vesting pool (transfers to EVM contract)")
	public ResponseEntity<ApiResponse<VestingPoolResponse>> fundPool(
			@PathVariable Long id,
			@Valid @RequestBody FundRequest request,
			@AuthenticationPrincipal Long adminId) {

		VestingPoolResponse pool = vestingService.fundPool(id, request.getAmount());
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_POOL_FUNDED,
				"VESTING_POOL", id,
				String.format("Funded pool with %s tokens", request.getAmount()));

		return ResponseEntity.ok(ApiResponse.success("Pool funded successfully", pool));
	}

	@GetMapping("/{id}/investors")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "List investors", description = "Get paginated list of investors in a vesting pool")
	public ResponseEntity<ApiResponse<Page<InvestorResponse>>> listInvestors(
			@PathVariable Long id,
			@PageableDefault(size = 50) Pageable pageable,
			@AuthenticationPrincipal Long adminId) {

		Page<InvestorResponse> investors = vestingService.listInvestors(id, pageable);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_INVESTOR_VIEWED,
				"VESTING_POOL", id, "Listed investors in pool");

		return ResponseEntity.ok(ApiResponse.success("Investors retrieved successfully", investors));
	}

	@PostMapping("/{id}/investors")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
	@Operation(summary = "Add investor", description = "Add an investor to a vesting pool (adds to EVM contract)")
	public ResponseEntity<ApiResponse<InvestorResponse>> addInvestor(
			@PathVariable Long id,
			@Valid @RequestBody InvestorRequest request,
			@AuthenticationPrincipal Long adminId) {

		InvestorResponse investor = vestingService.addInvestor(id, request);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_INVESTOR_ADDED,
				"VESTING_POOL", id,
				String.format("Added investor: userId=%d, allocation=%s",
						request.getUserId(), request.getAllocation()));

		return ResponseEntity.ok(ApiResponse.success("Investor added successfully", investor));
	}

	@GetMapping("/{id}/investors/{investorId}/claims")
	@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
	@Operation(summary = "Get investor claim status", description = "Get claim status and history for an investor (queries EVM contract)")
	public ResponseEntity<ApiResponse<ClaimStatusResponse>> getInvestorClaimStatus(
			@PathVariable Long id,
			@PathVariable Long investorId,
			@AuthenticationPrincipal Long adminId) {

		ClaimStatusResponse claimStatus = vestingService.getInvestorClaimStatus(id, investorId);
		auditService.logAction(adminId, null,
				com.tuttime.admin.entity.AdminAuditLog.AdminActionType.VESTING_CLAIM_STATUS_VIEWED,
				"VESTING_POOL", id,
				String.format("Viewed claim status for investor: %d", investorId));

		return ResponseEntity.ok(ApiResponse.success("Claim status retrieved successfully", claimStatus));
	}
}

