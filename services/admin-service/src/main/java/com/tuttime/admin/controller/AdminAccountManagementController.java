package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminAccountManagementService;
import com.tuttime.common.dto.ApiResponse;
import com.tuttime.common.exception.BusinessException;
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
 * Admin Account Management Controller
 * 
 * <p>Provides endpoints for managing admin users (admin accounts):
 * <ul>
 *   <li>List admin users with filtering</li>
 *   <li>View admin user details</li>
 *   <li>Create new admin users</li>
 *   <li>Update admin users</li>
 *   <li>Delete admin users</li>
 *   <li>Activate/deactivate admin accounts</li>
 *   <li>Unlock admin accounts</li>
 *   <li>Password management</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/admin-users")
@RequiredArgsConstructor
@Tag(name = "Admin Account Management", description = "Admin user (account) management APIs")
public class AdminAccountManagementController {

    private final AdminAccountManagementService adminAccountManagementService;
    private final AdminAuditService auditService;

    // ==================== CRUD Operations ====================

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List admin users", description = "Get paginated list of admin users with filtering and search")
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryResponse>>> listAdminUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {

        Page<AdminUserSummaryResponse> adminUsers = adminAccountManagementService.listAdminUsers(
                search, role, isActive, pageable);

        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ADMIN_USER_VIEWED,
                "ADMIN_USER", null, "Listed admin users");

        return ResponseEntity.ok(ApiResponse.success("Admin users retrieved successfully", adminUsers));
    }

    @GetMapping("/{adminId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get admin user details", description = "Get detailed information about a specific admin user")
    public ResponseEntity<ApiResponse<AdminUserResponse>> getAdminUser(
            @PathVariable Long adminId,
            @AuthenticationPrincipal Long currentAdminId) {

        AdminUserResponse adminUser = adminAccountManagementService.getAdminUser(adminId);

        auditService.logAction(currentAdminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.ADMIN_USER_VIEWED,
                "ADMIN_USER", adminId, "Viewed admin user details");

        return ResponseEntity.ok(ApiResponse.success("Admin user retrieved successfully", adminUser));
    }

    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Create admin user", description = "Create a new admin user (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> createAdminUser(
            @Valid @RequestBody CreateAdminUserRequest request,
            @AuthenticationPrincipal Long adminId) {

        AdminUserResponse adminUser = adminAccountManagementService.createAdminUser(request, adminId);

        return ResponseEntity.ok(ApiResponse.success("Admin user created successfully", adminUser));
    }

    @PutMapping("/{adminId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update admin user", description = "Update admin user information")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateAdminUser(
            @PathVariable Long adminId,
            @Valid @RequestBody UpdateAdminUserRequest request,
            @AuthenticationPrincipal Long currentAdminId) {

        AdminUserResponse adminUser = adminAccountManagementService.updateAdminUser(
                adminId, request, currentAdminId);

        return ResponseEntity.ok(ApiResponse.success("Admin user updated successfully", adminUser));
    }

    @DeleteMapping("/{adminId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete admin user", description = "Delete an admin user (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteAdminUser(
            @PathVariable Long adminId,
            @AuthenticationPrincipal Long currentAdminId) {

        adminAccountManagementService.deleteAdminUser(adminId, currentAdminId);

        return ResponseEntity.ok(ApiResponse.success("Admin user deleted successfully", null));
    }

    // ==================== Account Management ====================

    @PostMapping("/{adminId}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Activate admin user", description = "Activate an admin user account")
    public ResponseEntity<ApiResponse<AdminUserResponse>> activateAdminUser(
            @PathVariable Long adminId,
            @AuthenticationPrincipal Long currentAdminId) {

        AdminUserResponse adminUser = adminAccountManagementService.activateAdminUser(adminId, currentAdminId);

        return ResponseEntity.ok(ApiResponse.success("Admin user activated successfully", adminUser));
    }

    @PostMapping("/{adminId}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Deactivate admin user", description = "Deactivate an admin user account")
    public ResponseEntity<ApiResponse<AdminUserResponse>> deactivateAdminUser(
            @PathVariable Long adminId,
            @AuthenticationPrincipal Long currentAdminId) {

        AdminUserResponse adminUser = adminAccountManagementService.deactivateAdminUser(adminId, currentAdminId);

        return ResponseEntity.ok(ApiResponse.success("Admin user deactivated successfully", adminUser));
    }

    @PostMapping("/{adminId}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Unlock admin account", description = "Unlock a locked admin account (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> unlockAdminUser(
            @PathVariable Long adminId,
            @AuthenticationPrincipal Long currentAdminId) {

        AdminUserResponse adminUser = adminAccountManagementService.unlockAdminUser(adminId, currentAdminId);

        return ResponseEntity.ok(ApiResponse.success("Admin account unlocked successfully", adminUser));
    }

    @PostMapping("/{adminId}/reset-login-attempts")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reset failed login attempts", description = "Reset failed login attempts counter (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> resetFailedLoginAttempts(
            @PathVariable Long adminId,
            @AuthenticationPrincipal Long currentAdminId) {

        adminAccountManagementService.resetFailedLoginAttempts(adminId, currentAdminId);

        return ResponseEntity.ok(ApiResponse.success("Failed login attempts reset successfully", null));
    }

    // ==================== Password Management ====================

    @PostMapping("/{adminId}/change-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Change password", description = "Change own password (admin can only change their own password, requires current password)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long adminId,
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal Long currentAdminId) {

        // Ensure admin can only change their own password
        // SUPER_ADMIN should use reset-password endpoint instead
        if (!adminId.equals(currentAdminId)) {
            throw new BusinessException(
                    "You can only change your own password. Use reset-password endpoint to change other admin's password.");
        }

        adminAccountManagementService.changePassword(adminId, request);

        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/{adminId}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Reset password", description = "Reset admin user password (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long adminId,
            @Valid @RequestBody ResetPasswordRequest request,
            @AuthenticationPrincipal Long currentAdminId) {

        adminAccountManagementService.resetPassword(adminId, request, currentAdminId);

        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }
}

