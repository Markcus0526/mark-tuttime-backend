package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiResponse;
import com.tuttime.user.dto.*;
import com.tuttime.user.service.AdminUserService;
import com.tuttime.user.service.UserManagementPortalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Admin User Management Controller: provides admin-only endpoints for user management (called by admin-service via REST). */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "Admin-only user management APIs")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserManagementPortalService userManagementPortalService;

    @GetMapping
    @Operation(summary = "List users", description = "Get paginated list of users with filtering and search. Use page and size query parameters for pagination (e.g., ?page=0&size=100). Set includeDeleted=true to include soft-deleted users.")
    public ResponseEntity<ApiResponse<Page<UserResponse>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kycStatus,
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted,
            @PageableDefault(size = 100, page = 0) Pageable pageable) {
        
        Page<UserResponse> users = adminUserService.listUsers(search, status, kycStatus, includeDeleted, pageable);
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users));
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID", description = "Get basic user details by user ID. Set includeDeleted=true to allow getting soft-deleted users.")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted) {
        UserResponse user = adminUserService.getUserById(userId, includeDeleted);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @GetMapping("/{userId}/detail")
    @Operation(summary = "Get extended user detail", 
               description = "Get comprehensive user detail including device info, mining status, referral info, and balances (without full history)")
    public ResponseEntity<ApiResponse<ExtendedUserDetail>> getExtendedUserDetail(@PathVariable Long userId) {
        ExtendedUserDetail detail = userManagementPortalService.getExtendedUserDetail(userId);
        return ResponseEntity.ok(ApiResponse.success("Extended user detail retrieved successfully", detail));
    }

    @PutMapping("/{userId}")
    @Operation(summary = "Update user", description = "Update user information")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        
        UserResponse user = adminUserService.updateUser(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    @PostMapping("/{userId}/block")
    @Operation(summary = "Block user", description = "Block a user account")
    public ResponseEntity<ApiResponse<UserResponse>> blockUser(
            @PathVariable Long userId,
            @Valid @RequestBody BlockUserRequest request) {
        
        UserResponse user = adminUserService.blockUser(userId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", user));
    }

    @PostMapping("/{userId}/unblock")
    @Operation(summary = "Unblock user", description = "Unblock a user account")
    public ResponseEntity<ApiResponse<UserResponse>> unblockUser(@PathVariable Long userId) {
        UserResponse user = adminUserService.unblockUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", user));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Soft delete a user account")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        adminUserService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @DeleteMapping("/{userId}/hard-delete")
    @Operation(summary = "Hard delete user", description = "Completely delete a user account and all related data (devices, blocks, etc.). Works for both active and soft-deleted users. This operation is irreversible.")
    public ResponseEntity<ApiResponse<Void>> hardDeleteUser(@PathVariable Long userId) {
        adminUserService.hardDeleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User hard deleted successfully", null));
    }

    @PostMapping("/{userId}/restore")
    @Operation(summary = "Restore soft-deleted user", description = "Restore a soft-deleted user account by clearing deletedAt and reactivating")
    public ResponseEntity<ApiResponse<UserResponse>> restoreUser(@PathVariable Long userId) {
        UserResponse user = adminUserService.restoreUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User restored successfully", user));
    }

    @GetMapping("/by-phone/{phoneNumber}")
    @Operation(summary = "Get user by phone number", description = "Get user details by phone number")
    public ResponseEntity<ApiResponse<UserResponse>> getUserByPhoneNumber(@PathVariable String phoneNumber) {
        UserResponse user = adminUserService.getUserByPhoneNumber(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user));
    }

    @GetMapping("/{userId}/referrals")
    @Operation(summary = "Get user referrals", description = "Get list of user's referrals")
    public ResponseEntity<ApiResponse<UserReferralsResponse>> getUserReferrals(@PathVariable Long userId) {
        UserReferralsResponse referrals = adminUserService.getUserReferrals(userId);
        return ResponseEntity.ok(ApiResponse.success("User referrals retrieved successfully", referrals));
    }

    @GetMapping("/blocklist")
    @Operation(summary = "Get blocked users", description = "Get list of all blocked users")
    public ResponseEntity<ApiResponse<Page<BlockedUserResponse>>> getBlockedUsers(
            @PageableDefault(size = 50) Pageable pageable) {
        
        Page<BlockedUserResponse> users = adminUserService.getBlockedUsers(pageable);
        return ResponseEntity.ok(ApiResponse.success("Blocked users retrieved successfully", users));
    }

    // ============================================================================
    // USER MANAGEMENT PORTAL ENDPOINTS
    // ============================================================================

    @GetMapping("/{userId}/overview")
    @Operation(summary = "Get comprehensive user profile overview", 
               description = "Retrieves a comprehensive overview of a user's profile including balances, mining, referrals, staking, and history")
    public ResponseEntity<ApiResponse<UserOverview>> getUserOverview(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "0") Integer conversionPage,
            @RequestParam(required = false, defaultValue = "10") Integer conversionSize,
            @RequestParam(required = false, defaultValue = "0") Integer transactionPage,
            @RequestParam(required = false, defaultValue = "10") Integer transactionSize,
            @RequestParam(required = false, defaultValue = "0") Integer boostPage,
            @RequestParam(required = false, defaultValue = "10") Integer boostSize) {
        
        UserOverview overview = userManagementPortalService.getUserOverview(
                userId, 
                conversionPage, conversionSize,
                transactionPage, transactionSize,
                boostPage, boostSize);
        return ResponseEntity.ok(ApiResponse.success("User overview retrieved successfully", overview));
    }

    @PostMapping("/{userId}/actions/reset-mining")
    @Operation(summary = "Reset user mining", description = "Resets the user's mining session and monthly accumulated AP credits")
    public ResponseEntity<ApiResponse<String>> resetMining(
            @PathVariable Long userId,
            @RequestBody(required = false) ResetMiningRequest request) {
        
        if (request == null) {
            request = ResetMiningRequest.builder().build();
        }
        userManagementPortalService.resetMining(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Mining reset successfully", "Mining reset completed"));
    }

    @PostMapping("/{userId}/actions/pause")
    @Operation(summary = "Pause user account", description = "Pauses a user account, preventing them from accessing the platform")
    public ResponseEntity<ApiResponse<String>> pauseAccount(
            @PathVariable Long userId,
            @RequestBody(required = false) PauseAccountRequest request) {
        
        if (request == null) {
            request = PauseAccountRequest.builder().build();
        }
        userManagementPortalService.pauseAccount(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Account paused successfully", "Account has been paused"));
    }

    @PostMapping("/{userId}/actions/unpause")
    @Operation(summary = "Unpause user account", description = "Reactivates a previously paused user account")
    public ResponseEntity<ApiResponse<String>> unpauseAccount(@PathVariable Long userId) {
        userManagementPortalService.unpauseAccount(userId);
        return ResponseEntity.ok(ApiResponse.success("Account unpaused successfully", "Account has been reactivated"));
    }

    @PostMapping("/{userId}/actions/force-kyc")
    @Operation(summary = "Force KYC verification", description = "Manually marks a user's KYC as completed or resets it")
    public ResponseEntity<ApiResponse<String>> forceKyc(
            @PathVariable Long userId,
            @Valid @RequestBody ForceKycRequest request) {
        
        userManagementPortalService.forceKyc(userId, request);
        return ResponseEntity.ok(ApiResponse.success("KYC status updated successfully", 
                "KYC status has been " + (request.getCompleted() ? "completed" : "reset")));
    }

    @PostMapping("/{userId}/actions/add-bonus")
    @Operation(summary = "Add bonus to user account", description = "Adds a bonus (AP Credits or APART tokens) to a user's account")
    public ResponseEntity<ApiResponse<BonusResponse>> addBonus(
            @PathVariable Long userId,
            @Valid @RequestBody AddBonusRequest request) {
        
        BonusResponse response = userManagementPortalService.addBonus(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Bonus added successfully", response));
    }

    @PostMapping("/{userId}/actions/freeze-withdrawal")
    @Operation(summary = "Freeze user withdrawals", description = "Freezes a user's ability to withdraw tokens")
    public ResponseEntity<ApiResponse<String>> freezeWithdrawal(
            @PathVariable Long userId,
            @RequestBody(required = false) FreezeWithdrawalRequest request) {
        
        if (request == null) {
            request = FreezeWithdrawalRequest.builder().build();
        }
        userManagementPortalService.freezeWithdrawal(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal frozen successfully", "Withdrawals have been frozen"));
    }

    @PostMapping("/{userId}/actions/unfreeze-withdrawal")
    @Operation(summary = "Unfreeze user withdrawals", description = "Removes the withdrawal freeze from a user's account")
    public ResponseEntity<ApiResponse<String>> unfreezeWithdrawal(@PathVariable Long userId) {
        userManagementPortalService.unfreezeWithdrawal(userId);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal unfrozen successfully", "Withdrawals have been unfrozen"));
    }

    @PostMapping("/{userId}/actions/mark-anti-fraud")
    @Operation(summary = "Mark user for anti-fraud watch", description = "Marks a user account for enhanced anti-fraud monitoring")
    public ResponseEntity<ApiResponse<String>> markAntiFraud(
            @PathVariable Long userId,
            @RequestBody(required = false) MarkAntiFraudRequest request) {
        
        if (request == null) {
            request = MarkAntiFraudRequest.builder().build();
        }
        userManagementPortalService.markAntiFraud(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User marked for anti-fraud watch successfully", 
                "User has been marked for anti-fraud monitoring"));
    }

    @PostMapping("/{userId}/actions/unmark-anti-fraud")
    @Operation(summary = "Remove anti-fraud watch from user", description = "Removes the anti-fraud watch flag from a user account")
    public ResponseEntity<ApiResponse<String>> unmarkAntiFraud(@PathVariable Long userId) {
        userManagementPortalService.unmarkAntiFraud(userId);
        return ResponseEntity.ok(ApiResponse.success("Anti-fraud watch removed successfully", 
                "Anti-fraud watch has been removed"));
    }

    @PostMapping("/preferred-languages")
    @Operation(summary = "Get preferred languages for multiple users", 
               description = "Bulk endpoint to get preferred language codes for a list of user IDs. Returns a map of userId -> languageCode")
    public ResponseEntity<ApiResponse<Map<Long, String>>> getUsersPreferredLanguages(
            @RequestBody List<Long> userIds) {
        Map<Long, String> languages = adminUserService.getUsersPreferredLanguages(userIds);
        return ResponseEntity.ok(ApiResponse.success("Preferred languages retrieved successfully", languages));
    }

    @PostMapping("/ids")
    @Operation(summary = "Get all active user IDs", 
               description = "Bulk endpoint to get all active user IDs (returns only IDs, not full user objects). Much more efficient than paginating through full user objects.")
    public ResponseEntity<ApiResponse<List<Long>>> getAllActiveUserIds(
            @RequestParam(required = false, defaultValue = "active") String status,
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted) {
        List<Long> userIds = adminUserService.getAllActiveUserIds(status, includeDeleted);
        return ResponseEntity.ok(ApiResponse.success("User IDs retrieved successfully", userIds));
    }
}

