package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminTokenService;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Admin Token Management Controller
 * 
 * <p>Provides endpoints for managing APART tokens:
 * <ul>
 *   <li>View token balances</li>
 *   <li>Adjust token balances</li>
 *   <li>View token transactions</li>
 *   <li>Burn tokens</li>
 *   <li>View user status levels</li>
 * </ul>
 * Returns static values when token-service is unavailable
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tokens")
@RequiredArgsConstructor
@Tag(name = "Admin Token Management", description = "APART token management APIs")
public class AdminTokenController {

    private final AdminTokenService tokenService;
    private final AdminAuditService auditService;

    @GetMapping("/users/{userId}/balance")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user token balance", description = "Get APART token balance for a user")
    public ResponseEntity<ApiResponse<TokenBalanceResponse>> getUserTokenBalance(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        TokenBalanceResponse balance = tokenService.getUserTokenBalance(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_TRANSACTION_VIEWED,
                "TOKEN", userId, "Viewed token balance");
        
        return ResponseEntity.ok(ApiResponse.success("Token balance retrieved successfully", balance));
    }

    @PostMapping("/users/{userId}/balance/adjust")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Adjust token balance", description = "Adjust user's APART token balance (add or subtract)")
    public ResponseEntity<ApiResponse<TokenBalanceResponse>> adjustTokenBalance(
            @PathVariable Long userId,
            @Valid @RequestBody AdjustTokenBalanceRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        TokenBalanceResponse balance = tokenService.adjustTokenBalance(userId, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_BALANCE_ADJUSTED,
                "TOKEN", userId, "Adjusted token balance: " + request.getAmount() + ", reason: " + request.getReason());
        
        return ResponseEntity.ok(ApiResponse.success("Token balance adjusted successfully", balance));
    }

    @GetMapping("/users/{userId}/transactions")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get token transactions", description = "Get token transaction history for a user with optional filters")
    public ResponseEntity<ApiResponse<Page<TokenTransactionResponse>>> getTokenTransactions(
            @PathVariable Long userId,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<TokenTransactionResponse> transactions = tokenService.getTokenTransactions(userId, type, status, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_TRANSACTION_VIEWED,
                "TOKEN", userId, "Viewed token transactions");
        
        return ResponseEntity.ok(ApiResponse.success("Token transactions retrieved successfully", transactions));
    }

    @PostMapping("/users/{userId}/burn")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Burn tokens", description = "Burn APART tokens from a user")
    public ResponseEntity<ApiResponse<TokenBalanceResponse>> burnTokens(
            @PathVariable Long userId,
            @Valid @RequestBody BurnTokensRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        TokenBalanceResponse balance = tokenService.burnTokens(userId, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_BURNED,
                "TOKEN", userId, "Burned tokens: " + request.getAmount() + ", reason: " + request.getReason());
        
        return ResponseEntity.ok(ApiResponse.success("Tokens burned successfully", balance));
    }

    @GetMapping("/users/{userId}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user status level", description = "Get user's status level based on token balance")
    public ResponseEntity<ApiResponse<UserStatusResponse>> getUserStatus(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        UserStatusResponse status = tokenService.getUserStatus(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.STATUS_UPDATED,
                "TOKEN", userId, "Viewed user status");
        
        return ResponseEntity.ok(ApiResponse.success("User status retrieved successfully", status));
    }

    // ==================== Bulk Operations ====================

    @PostMapping("/bulk/adjust")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Bulk adjust token balances", description = "Adjust token balances for multiple users at once (max 1000 users)")
    public ResponseEntity<ApiResponse<BulkOperationResponse>> bulkAdjustTokenBalances(
            @Valid @RequestBody BulkTokenAdjustmentRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        BulkOperationResponse response = tokenService.bulkAdjustTokenBalances(request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_BALANCE_ADJUSTED,
                "TOKEN", null,
                String.format("Bulk adjusted token balances for %d users: %d successful, %d failed. Amount: %s, Type: %s, Reason: %s",
                        response.getTotalRequested(), response.getSuccessful(), response.getFailed(),
                        request.getAmount(), request.getType(), request.getReason()));
        
        return ResponseEntity.ok(ApiResponse.success("Bulk token adjustment completed", response));
    }

    // ==================== Transaction Histories ====================

    @GetMapping("/transactions/deposits")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get deposit history", description = "Get global deposit transaction history with filters")
    public ResponseEntity<ApiResponse<Page<GlobalTransactionResponse>>> getDepositHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<GlobalTransactionResponse> history = tokenService.getDepositHistory(status, network, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_TRANSACTION_VIEWED,
                "TOKEN", null, "Viewed deposit history");
        
        return ResponseEntity.ok(ApiResponse.success("Deposit history retrieved successfully", history));
    }

    @GetMapping("/transactions/withdrawals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get withdrawal history", description = "Get global withdrawal transaction history with filters")
    public ResponseEntity<ApiResponse<Page<GlobalTransactionResponse>>> getWithdrawalHistory(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String network,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<GlobalTransactionResponse> history = tokenService.getWithdrawalHistory(status, network, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_TRANSACTION_VIEWED,
                "TOKEN", null, "Viewed withdrawal history");
        
        return ResponseEntity.ok(ApiResponse.success("Withdrawal history retrieved successfully", history));
    }

    @GetMapping("/transactions/burns")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get burn history", description = "Get global token burn history with filters")
    public ResponseEntity<ApiResponse<Page<GlobalTransactionResponse>>> getBurnHistory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<GlobalTransactionResponse> history = tokenService.getBurnHistory(startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_TRANSACTION_VIEWED,
                "TOKEN", null, "Viewed burn history");
        
        return ResponseEntity.ok(ApiResponse.success("Burn history retrieved successfully", history));
    }

    @GetMapping("/transactions/claims")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get claim history", description = "Get staking reward claim history with filters")
    public ResponseEntity<ApiResponse<Page<StakingClaimResponse>>> getClaimHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<StakingClaimResponse> history = tokenService.getClaimHistory(userId, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.STAKING_VIEWED,
                "STAKING", null, "Viewed claim history");
        
        return ResponseEntity.ok(ApiResponse.success("Claim history retrieved successfully", history));
    }
}

