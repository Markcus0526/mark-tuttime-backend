package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.service.AdminUserManagementService;
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

import java.util.Map;

/**
 * Admin User Management Controller
 * 
 * <p>Provides endpoints for managing regular users:
 * <ul>
 *   <li>View users (list, search, filter)</li>
 *   <li>View user details</li>
 *   <li>Update user information</li>
 *   <li>Block/unblock users</li>
 *   <li>Approve/reject KYC</li>
 *   <li>Delete users</li>
 *   <li>View user referral network</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin User Management", description = "User management APIs for admin panel")
public class AdminUserManagementController {

    private final AdminUserManagementService userManagementService;
    private final AdminAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List users", description = "Get paginated list of users with filtering and search. Use page and size query parameters for pagination (e.g., ?page=0&size=100)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String kycStatus,
            @PageableDefault(size = 100, page = 0) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<UserSummaryResponse> users = userManagementService.listUsers(search, status, kycStatus, pageable);
        auditService.logAction(adminId, null, 
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_VIEWED,
                "USER", null, "Listed users");
        
        // Log pagination info for debugging
        log.info("Returning users page: totalElements={}, totalPages={}, number={}, size={}, contentSize={}, first={}, last={}", 
                users.getTotalElements(), users.getTotalPages(), users.getNumber(), 
                users.getSize(), users.getContent().size(), users.isFirst(), users.isLast());
        
        // Ensure Page object has all required fields for proper serialization
        if (users.getTotalElements() == 0 && users.getContent().isEmpty()) {
            log.warn("Page has 0 totalElements but content is empty - this might indicate a pagination issue");
        }
        
        // Convert Page to Map to ensure proper JSON serialization
        // Spring's PageImpl serialization can be unstable, so we manually create a Map
        Map<String, Object> pageMap = new java.util.HashMap<>();
        pageMap.put("content", users.getContent());
        pageMap.put("totalElements", users.getTotalElements());
        pageMap.put("totalPages", users.getTotalPages());
        pageMap.put("number", users.getNumber());
        pageMap.put("size", users.getSize());
        pageMap.put("numberOfElements", users.getNumberOfElements());
        pageMap.put("first", users.isFirst());
        pageMap.put("last", users.isLast());
        pageMap.put("empty", users.isEmpty());
        pageMap.put("pageable", Map.of(
            "pageNumber", users.getPageable().getPageNumber(),
            "pageSize", users.getPageable().getPageSize(),
            "paged", users.getPageable().isPaged()
        ));
        
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", pageMap));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user details", description = "Get detailed information about a specific user")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserDetails(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        UserDetailResponse user = userManagementService.getUserDetails(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_VIEWED,
                "USER", userId, "Viewed user details");
        
        return ResponseEntity.ok(ApiResponse.success("User details retrieved successfully", user));
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update user", description = "Update user information")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        UserDetailResponse user = userManagementService.updateUser(userId, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UPDATED,
                "USER", userId, "Updated user: " + request.toString());
        
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user));
    }

    @PostMapping("/{userId}/block")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Block user", description = "Block a user account")
    public ResponseEntity<ApiResponse<UserDetailResponse>> blockUser(
            @PathVariable Long userId,
            @Valid @RequestBody BlockUserRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        UserDetailResponse user = userManagementService.blockUser(userId, 
                request != null ? request.getReason() : null);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_BLOCKED,
                "USER", userId, "Blocked user: " + (request != null ? request.getReason() : "No reason"));
        
        return ResponseEntity.ok(ApiResponse.success("User blocked successfully", user));
    }

    @PostMapping("/{userId}/unblock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Unblock user", description = "Unblock a user account")
    public ResponseEntity<ApiResponse<UserDetailResponse>> unblockUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        UserDetailResponse user = userManagementService.unblockUser(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UNBLOCKED,
                "USER", userId, "Unblocked user");
        
        return ResponseEntity.ok(ApiResponse.success("User unblocked successfully", user));
    }

    @PostMapping("/{userId}/kyc/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Approve KYC", description = "Approve user KYC verification")
    public ResponseEntity<ApiResponse<UserDetailResponse>> approveKyc(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        UserDetailResponse user = userManagementService.approveKyc(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_KYC_APPROVED,
                "USER", userId, "Approved KYC");
        
        return ResponseEntity.ok(ApiResponse.success("KYC approved successfully", user));
    }

    @PostMapping("/{userId}/kyc/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Reject KYC", description = "Reject user KYC verification")
    public ResponseEntity<ApiResponse<UserDetailResponse>> rejectKyc(
            @PathVariable Long userId,
            @RequestBody RejectKycRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        UserDetailResponse user = userManagementService.rejectKyc(userId, request.getReason());
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_KYC_REJECTED,
                "USER", userId, "Rejected KYC: " + request.getReason());
        
        return ResponseEntity.ok(ApiResponse.success("KYC rejected", user));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Delete user", description = "Soft delete a user account (SUPER_ADMIN only)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        userManagementService.deleteUser(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_DELETED,
                "USER", userId, "Deleted user");
        
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null));
    }

    @GetMapping("/{userId}/referrals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user referral network", description = "Get referral network tree for a user")
    public ResponseEntity<ApiResponse<ReferralNetworkResponse>> getUserReferrals(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        ReferralNetworkResponse referrals = userManagementService.getUserReferrals(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.REFERRAL_VIEWED,
                "USER", userId, "Viewed referral network");
        
        return ResponseEntity.ok(ApiResponse.success("Referral network retrieved successfully", referrals));
    }

    @PostMapping("/referrals")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create referral", description = "Create a referral relationship between two users. Level is optional and will be auto-calculated if not provided.")
    public ResponseEntity<ApiResponse<String>> createReferral(
            @RequestParam Long referrerId,
            @RequestParam Long refereeId,
            @RequestParam(required = false) Integer level,
            @AuthenticationPrincipal Long adminId) {
        
        String result = userManagementService.createReferral(referrerId, refereeId, level);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.REFERRAL_CREATED,
                "USER", refereeId, 
                String.format("Created referral: referrerId=%d, refereeId=%d, level=%s", 
                        referrerId, refereeId, level != null ? level : "auto"));
        
        return ResponseEntity.ok(ApiResponse.success("Referral created successfully", result));
    }

    // ==================== Bulk Operations ====================

    @PostMapping("/bulk/block")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Bulk block users", description = "Block multiple users at once (max 1000 users)")
    public ResponseEntity<ApiResponse<BulkOperationResponse>> bulkBlockUsers(
            @Valid @RequestBody BulkOperationRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        BulkOperationResponse response = userManagementService.bulkBlockUsers(request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_BLOCKED,
                "USER", null, 
                String.format("Bulk blocked %d users: %d successful, %d failed", 
                        response.getTotalRequested(), response.getSuccessful(), response.getFailed()));
        
        return ResponseEntity.ok(ApiResponse.success("Bulk block operation completed", response));
    }

    @PostMapping("/bulk/unblock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Bulk unblock users", description = "Unblock multiple users at once (max 1000 users)")
    public ResponseEntity<ApiResponse<BulkOperationResponse>> bulkUnblockUsers(
            @Valid @RequestBody BulkOperationRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        BulkOperationResponse response = userManagementService.bulkUnblockUsers(request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UNBLOCKED,
                "USER", null,
                String.format("Bulk unblocked %d users: %d successful, %d failed",
                        response.getTotalRequested(), response.getSuccessful(), response.getFailed()));
        
        return ResponseEntity.ok(ApiResponse.success("Bulk unblock operation completed", response));
    }

    @PostMapping("/bulk/kyc/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Bulk approve KYC", description = "Approve KYC for multiple users at once (max 1000 users)")
    public ResponseEntity<ApiResponse<BulkOperationResponse>> bulkApproveKyc(
            @Valid @RequestBody BulkOperationRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        BulkOperationResponse response = userManagementService.bulkApproveKyc(request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_KYC_APPROVED,
                "USER", null,
                String.format("Bulk approved KYC for %d users: %d successful, %d failed",
                        response.getTotalRequested(), response.getSuccessful(), response.getFailed()));
        
        return ResponseEntity.ok(ApiResponse.success("Bulk KYC approval completed", response));
    }

    @PostMapping("/bulk/kyc/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Bulk reject KYC", description = "Reject KYC for multiple users at once (max 1000 users)")
    public ResponseEntity<ApiResponse<BulkOperationResponse>> bulkRejectKyc(
            @Valid @RequestBody BulkOperationRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        BulkOperationResponse response = userManagementService.bulkRejectKyc(request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_KYC_REJECTED,
                "USER", null,
                String.format("Bulk rejected KYC for %d users: %d successful, %d failed. Reason: %s",
                        response.getTotalRequested(), response.getSuccessful(), response.getFailed(),
                        request.getReason()));
        
        return ResponseEntity.ok(ApiResponse.success("Bulk KYC rejection completed", response));
    }

    @PostMapping("/bulk/delete")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Bulk delete users", description = "Delete multiple users at once (SUPER_ADMIN only, max 1000 users)")
    public ResponseEntity<ApiResponse<BulkOperationResponse>> bulkDeleteUsers(
            @Valid @RequestBody BulkOperationRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        BulkOperationResponse response = userManagementService.bulkDeleteUsers(request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_DELETED,
                "USER", null,
                String.format("Bulk deleted %d users: %d successful, %d failed",
                        response.getTotalRequested(), response.getSuccessful(), response.getFailed()));
        
        return ResponseEntity.ok(ApiResponse.success("Bulk delete operation completed", response));
    }

    // ==================== User Management Portal Endpoints ====================

    @GetMapping("/{userId}/overview")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user overview", description = "Get comprehensive user overview with all data including history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserOverview(
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "0") Integer conversionPage,
            @RequestParam(required = false, defaultValue = "10") Integer conversionSize,
            @RequestParam(required = false, defaultValue = "0") Integer transactionPage,
            @RequestParam(required = false, defaultValue = "10") Integer transactionSize,
            @RequestParam(required = false, defaultValue = "0") Integer boostPage,
            @RequestParam(required = false, defaultValue = "10") Integer boostSize,
            @AuthenticationPrincipal Long adminId) {
        
        Map<String, Object> overview = userManagementService.getUserOverview(userId,
                conversionPage, conversionSize,
                transactionPage, transactionSize,
                boostPage, boostSize);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_VIEWED,
                "USER", userId, "Viewed user overview");
        
        return ResponseEntity.ok(ApiResponse.success("User overview retrieved successfully", overview));
    }

    @GetMapping("/{userId}/detail")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get extended user detail", description = "Get comprehensive user detail without full history")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExtendedUserDetail(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        Map<String, Object> detail = userManagementService.getExtendedUserDetail(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_VIEWED,
                "USER", userId, "Viewed extended user detail");
        
        return ResponseEntity.ok(ApiResponse.success("Extended user detail retrieved successfully", detail));
    }

    @PostMapping("/{userId}/actions/reset-mining")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Reset user mining", description = "Reset user mining session and accumulated credits")
    public ResponseEntity<ApiResponse<String>> resetMining(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        
        userManagementService.resetMining(userId, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UPDATED,
                "USER", userId, "Reset mining");
        
        return ResponseEntity.ok(ApiResponse.success("Mining reset successfully", "Mining reset completed"));
    }

    @PostMapping("/{userId}/actions/pause")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Pause user account", description = "Pause a user account")
    public ResponseEntity<ApiResponse<String>> pauseAccount(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        
        userManagementService.pauseAccount(userId, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_BLOCKED,
                "USER", userId, "Paused account");
        
        return ResponseEntity.ok(ApiResponse.success("Account paused successfully", "Account has been paused"));
    }

    @PostMapping("/{userId}/actions/unpause")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Unpause user account", description = "Reactivate a paused user account")
    public ResponseEntity<ApiResponse<String>> unpauseAccount(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        userManagementService.unpauseAccount(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UNBLOCKED,
                "USER", userId, "Unpaused account");
        
        return ResponseEntity.ok(ApiResponse.success("Account unpaused successfully", "Account has been reactivated"));
    }

    @PostMapping("/{userId}/actions/add-bonus")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Add bonus to user", description = "Add AP Credits or APART tokens bonus to user account")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addBonus(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        
        Map<String, Object> response = userManagementService.addBonus(userId, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UPDATED,
                "USER", userId, "Added bonus: " + request.get("bonusType"));
        
        return ResponseEntity.ok(ApiResponse.success("Bonus added successfully", response));
    }

    @PostMapping("/{userId}/actions/freeze-withdrawal")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Freeze user withdrawals", description = "Freeze user's ability to withdraw tokens")
    public ResponseEntity<ApiResponse<String>> freezeWithdrawal(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        
        userManagementService.freezeWithdrawal(userId, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UPDATED,
                "USER", userId, "Froze withdrawals");
        
        return ResponseEntity.ok(ApiResponse.success("Withdrawal frozen successfully", "Withdrawals have been frozen"));
    }

    @PostMapping("/{userId}/actions/unfreeze-withdrawal")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Unfreeze user withdrawals", description = "Remove withdrawal freeze from user account")
    public ResponseEntity<ApiResponse<String>> unfreezeWithdrawal(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        userManagementService.unfreezeWithdrawal(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UPDATED,
                "USER", userId, "Unfroze withdrawals");
        
        return ResponseEntity.ok(ApiResponse.success("Withdrawal unfrozen successfully", "Withdrawals have been unfrozen"));
    }

    @PostMapping("/{userId}/actions/mark-anti-fraud")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Mark user for anti-fraud watch", description = "Mark user account for enhanced anti-fraud monitoring")
    public ResponseEntity<ApiResponse<String>> markAntiFraud(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        
        userManagementService.markAntiFraud(userId, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UPDATED,
                "USER", userId, "Marked for anti-fraud watch");
        
        return ResponseEntity.ok(ApiResponse.success("User marked for anti-fraud watch successfully",
                "User has been marked for anti-fraud monitoring"));
    }

    @PostMapping("/{userId}/actions/unmark-anti-fraud")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Remove anti-fraud watch", description = "Remove anti-fraud watch flag from user account")
    public ResponseEntity<ApiResponse<String>> unmarkAntiFraud(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        userManagementService.unmarkAntiFraud(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_UPDATED,
                "USER", userId, "Removed anti-fraud watch");
        
        return ResponseEntity.ok(ApiResponse.success("Anti-fraud watch removed successfully",
                "Anti-fraud watch has been removed"));
    }

    // ==================== Frontend-Compatible Endpoints ====================
    // These endpoints match the frontend API calls for backward compatibility

    @PostMapping("/{userId}/mining/reset")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Reset user mining (frontend compatible)", description = "Reset user mining session and accumulated credits")
    public ResponseEntity<ApiResponse<String>> resetMiningFrontend(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        return resetMining(userId, request, adminId);
    }

    @PostMapping("/{userId}/pause")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Pause user account (frontend compatible)", description = "Pause a user account")
    public ResponseEntity<ApiResponse<String>> pauseAccountFrontend(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        return pauseAccount(userId, request, adminId);
    }

    @PostMapping("/{userId}/unpause")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Unpause user account (frontend compatible)", description = "Unpause a user account")
    public ResponseEntity<ApiResponse<String>> unpauseAccountFrontend(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        return unpauseAccount(userId, adminId);
    }

    @PostMapping("/{userId}/bonus")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Add bonus to user (frontend compatible)", description = "Add AP or APART bonus to user")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addBonusFrontend(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        return addBonus(userId, request, adminId);
    }

    @PostMapping("/{userId}/withdrawal/freeze")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Freeze user withdrawal (frontend compatible)", description = "Freeze user withdrawal capability")
    public ResponseEntity<ApiResponse<String>> freezeWithdrawalFrontend(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        return freezeWithdrawal(userId, request, adminId);
    }

    @PostMapping("/{userId}/withdrawal/unfreeze")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Unfreeze user withdrawal (frontend compatible)", description = "Unfreeze user withdrawal capability")
    public ResponseEntity<ApiResponse<String>> unfreezeWithdrawalFrontend(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        return unfreezeWithdrawal(userId, adminId);
    }

    @PostMapping("/{userId}/antifraud/mark")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Mark user for anti-fraud watch (frontend compatible)", description = "Mark user for anti-fraud monitoring")
    public ResponseEntity<ApiResponse<String>> markAntiFraudFrontend(
            @PathVariable Long userId,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal Long adminId) {
        return markAntiFraud(userId, request, adminId);
    }

    @PostMapping("/{userId}/antifraud/unmark")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Unmark user from anti-fraud watch (frontend compatible)", description = "Remove user from anti-fraud watchlist")
    public ResponseEntity<ApiResponse<String>> unmarkAntiFraudFrontend(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        return unmarkAntiFraud(userId, adminId);
    }

    // ==================== Beta Tester Endpoints ====================

    @PostMapping("/beta-tester/delete-by-phone")
    @Operation(summary = "Delete beta tester by phone number", description = "Public endpoint for beta testers. Hard deletes all user data including cache, devices, tokens, and records from all services")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteBetaTesterByPhone(
            @RequestParam String phoneNumber) {
        
        Map<String, Object> result = userManagementService.deleteBetaTesterByPhone(phoneNumber);
        return ResponseEntity.ok(ApiResponse.success("Beta tester deleted successfully", result));
    }
}

