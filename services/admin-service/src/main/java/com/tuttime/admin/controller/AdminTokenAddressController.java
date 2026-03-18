package com.tuttime.admin.controller;

import com.tuttime.admin.dto.BlockedAddressResponse;
import com.tuttime.admin.dto.BlockIpRequest;
import com.tuttime.admin.dto.WhitelistedAddressResponse;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminTokenAddressService;
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
 * Admin Token Address Blocklist/Whitelist Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tokens/addresses")
@RequiredArgsConstructor
@Tag(name = "Admin Token Address Management", description = "Token address blocklist/whitelist management APIs")
public class AdminTokenAddressController {

    private final AdminTokenAddressService addressService;
    private final AdminAuditService auditService;

    @GetMapping("/blocked")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List blocked addresses", description = "Get paginated list of all blocked token/wallet addresses")
    public ResponseEntity<ApiResponse<Page<BlockedAddressResponse>>> listBlockedAddresses(
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<BlockedAddressResponse> addresses = addressService.listBlockedAddresses(pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ADDRESS_BLOCKLIST_VIEWED,
                "TOKEN_ADDRESS", null, "Listed blocked addresses");
        
        return ResponseEntity.ok(ApiResponse.success("Blocked addresses retrieved successfully", addresses));
    }

    @GetMapping("/whitelisted")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List whitelisted addresses", description = "Get paginated list of all whitelisted token/wallet addresses")
    public ResponseEntity<ApiResponse<Page<WhitelistedAddressResponse>>> listWhitelistedAddresses(
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<WhitelistedAddressResponse> addresses = addressService.listWhitelistedAddresses(pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ADDRESS_WHITELIST_VIEWED,
                "TOKEN_ADDRESS", null, "Listed whitelisted addresses");
        
        return ResponseEntity.ok(ApiResponse.success("Whitelisted addresses retrieved successfully", addresses));
    }

    @PostMapping("/{address}/block")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Block address", description = "Block a token/wallet address from transactions")
    public ResponseEntity<ApiResponse<Void>> blockAddress(
            @PathVariable String address,
            @RequestBody(required = false) BlockIpRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        addressService.blockAddress(address, request != null ? request.getReason() : null, adminId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ADDRESS_BLOCKED,
                "TOKEN_ADDRESS", null, "Blocked address: " + address);
        
        return ResponseEntity.ok(ApiResponse.success("Address blocked successfully", null));
    }

    @PostMapping("/{address}/unblock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Unblock address", description = "Unblock a token/wallet address")
    public ResponseEntity<ApiResponse<Void>> unblockAddress(
            @PathVariable String address,
            @AuthenticationPrincipal Long adminId) {
        
        addressService.unblockAddress(address);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ADDRESS_UNBLOCKED,
                "TOKEN_ADDRESS", null, "Unblocked address: " + address);
        
        return ResponseEntity.ok(ApiResponse.success("Address unblocked successfully", null));
    }

    @PostMapping("/{address}/whitelist")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Whitelist address", description = "Add a token/wallet address to the whitelist")
    public ResponseEntity<ApiResponse<Void>> whitelistAddress(
            @PathVariable String address,
            @RequestBody(required = false) BlockIpRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        addressService.whitelistAddress(address, request != null ? request.getReason() : null, adminId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ADDRESS_WHITELISTED,
                "TOKEN_ADDRESS", null, "Whitelisted address: " + address);
        
        return ResponseEntity.ok(ApiResponse.success("Address whitelisted successfully", null));
    }

    @PostMapping("/{address}/remove-whitelist")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Remove address from whitelist", description = "Remove a token/wallet address from the whitelist")
    public ResponseEntity<ApiResponse<Void>> removeAddressFromWhitelist(
            @PathVariable String address,
            @AuthenticationPrincipal Long adminId) {
        
        addressService.removeAddressFromWhitelist(address);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ADDRESS_WHITELIST_REMOVED,
                "TOKEN_ADDRESS", null, "Removed address from whitelist: " + address);
        
        return ResponseEntity.ok(ApiResponse.success("Address removed from whitelist successfully", null));
    }
}

