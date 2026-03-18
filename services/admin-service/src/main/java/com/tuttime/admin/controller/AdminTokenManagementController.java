package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminTokenManagementService;
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
 * Admin Token Management Controller (JWT Whitelist/Blacklist)
 * 
 * <p>Provides endpoints for managing JWT token whitelist and blacklist.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/token-management")
@RequiredArgsConstructor
@Tag(name = "Admin Token Management", description = "JWT token whitelist/blacklist management APIs")
public class AdminTokenManagementController {

    private final AdminTokenManagementService tokenManagementService;
    private final AdminAuditService auditService;

    @PostMapping("/users/{userId}/revoke-all")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Revoke all user tokens", description = "Revoke all JWT tokens for a user")
    public ResponseEntity<ApiResponse<TokenRevocationResponse>> revokeAllUserTokens(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        TokenRevocationResponse response = tokenManagementService.revokeAllUserTokens(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_BLACKLISTED,
                "TOKEN", userId, "Revoked all tokens for user");
        
        return ResponseEntity.ok(ApiResponse.success("All user tokens revoked successfully", response));
    }

    @PostMapping("/tokens/{jti}/revoke")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Revoke specific token", description = "Revoke a specific JWT token by JTI")
    public ResponseEntity<ApiResponse<Void>> revokeToken(
            @PathVariable String jti,
            @AuthenticationPrincipal Long adminId) {
        
        tokenManagementService.revokeToken(jti);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_BLACKLISTED,
                "TOKEN", null, "Revoked token: " + jti);
        
        return ResponseEntity.ok(ApiResponse.success("Token revoked successfully", null));
    }

    @GetMapping("/users/{userId}/tokens")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "List user tokens", description = "Get list of active tokens for a user")
    public ResponseEntity<ApiResponse<java.util.List<TokenInfoResponse>>> listUserTokens(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        java.util.List<TokenInfoResponse> tokens = tokenManagementService.listUserTokens(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TOKEN_WHITELISTED,
                "TOKEN", userId, "Listed user tokens");
        
        return ResponseEntity.ok(ApiResponse.success("User tokens retrieved successfully", tokens));
    }
}

