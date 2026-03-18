package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminStakingService;
import com.tuttime.admin.service.AdminTokenService;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

/**
 * Admin Staking Management Controller
 * Returns static values when staking-service is unavailable
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/staking")
@RequiredArgsConstructor
@Tag(name = "Admin Staking Management", description = "Staking management APIs")
public class AdminStakingController {

    private final AdminStakingService stakingService;
    private final AdminTokenService tokenService;
    private final AdminAuditService auditService;

    @GetMapping("/users/{userId}/positions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get staking positions", description = "Get staking positions for a user")
    public ResponseEntity<ApiResponse<StakingPositionsResponse>> getStakingPositions(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        StakingPositionsResponse positions = stakingService.getStakingPositions(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.STAKING_VIEWED,
                "STAKING", userId, "Viewed staking positions");
        
        return ResponseEntity.ok(ApiResponse.success("Staking positions retrieved successfully", positions));
    }

    @GetMapping("/pools")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List staking pools", description = "Get list of all staking pools")
    public ResponseEntity<ApiResponse<java.util.List<StakingPoolResponse>>> listStakingPools(
            @AuthenticationPrincipal Long adminId) {
        
        java.util.List<StakingPoolResponse> pools = stakingService.listStakingPools();
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.STAKING_VIEWED,
                "STAKING", null, "Listed staking pools");
        
        return ResponseEntity.ok(ApiResponse.success("Staking pools retrieved successfully", pools));
    }

    // ==================== Staking Histories ====================

    @GetMapping("/history/stakes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get staking history", description = "Get full history of stake operations with filters")
    public ResponseEntity<ApiResponse<Page<StakingHistoryResponse>>> getStakingHistory(
            @RequestParam(required = false) Long poolId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<StakingHistoryResponse> history = stakingService.getStakingHistory(poolId, userId, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.STAKING_VIEWED,
                "STAKING", null, "Viewed staking history");
        
        return ResponseEntity.ok(ApiResponse.success("Staking history retrieved successfully", history));
    }

    @GetMapping("/history/unstakes")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get unstaking history", description = "Get full history of unstake operations with filters")
    public ResponseEntity<ApiResponse<Page<StakingHistoryResponse>>> getUnstakingHistory(
            @RequestParam(required = false) Long poolId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<StakingHistoryResponse> history = stakingService.getUnstakingHistory(poolId, userId, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.STAKING_VIEWED,
                "STAKING", null, "Viewed unstaking history");
        
        return ResponseEntity.ok(ApiResponse.success("Unstaking history retrieved successfully", history));
    }

    @GetMapping("/history/claims")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get claim history", description = "Get full history of staking reward claims")
    public ResponseEntity<ApiResponse<Page<StakingClaimResponse>>> getClaimHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        // Note: Claim history is handled by token service, but exposed through staking controller for consistency
        Page<StakingClaimResponse> history = tokenService.getClaimHistory(userId, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.STAKING_VIEWED,
                "STAKING", null, "Viewed claim history");
        
        return ResponseEntity.ok(ApiResponse.success("Claim history retrieved successfully", history));
    }
}

