package com.tuttime.admin.controller;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.rest.client.NotificationServiceRestClient;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin Controller for User Group Management
 * Handles user group CRUD operations
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/notifications/user-groups")
@Tag(name = "Admin User Group Management", description = "Admin APIs for user group management")
@RequiredArgsConstructor
public class AdminNotificationUserGroupController {

    private final NotificationServiceRestClient notificationServiceRestClient;
    private final AdminAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List User Groups", description = "Get list of user groups")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listUserGroups(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Listing user groups", adminId);
        
        Map<String, Object> pageData = notificationServiceRestClient.listUserGroups(pageable);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_VIEWED,
                "USER_GROUP", null, "Listed user groups");
        
        return ResponseEntity.ok(ApiResponse.success("User groups retrieved successfully", pageData));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get User Group", description = "Get user group details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Getting user group: {}", adminId, id);
        
        Map<String, Object> group = notificationServiceRestClient.getUserGroup(id);
        
        return ResponseEntity.ok(ApiResponse.success("User group retrieved successfully", group));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create User Group", description = "Create a new user group")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createUserGroup(
            @Valid @RequestBody CreateUserGroupRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Creating user group: name={}", adminId, request.getGroupName());
        
        Map<String, Object> groupRequest = new HashMap<>();
        groupRequest.put("groupName", request.getGroupName());
        groupRequest.put("description", request.getDescription());
        groupRequest.put("criteria", request.getCriteria());
        
        Map<String, Object> group = notificationServiceRestClient.createUserGroup(groupRequest);
        
        Long groupId = null;
        if (group.get("groupId") != null) {
            Object idObj = group.get("groupId");
            if (idObj instanceof Number) {
                groupId = ((Number) idObj).longValue();
            }
        }
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_CREATED,
                "USER_GROUP", groupId,
                "Created user group: " + request.getGroupName());
        
        return ResponseEntity.ok(ApiResponse.success("User group created successfully", group));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update User Group", description = "Update user group")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserGroup(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserGroupRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Updating user group: {}", adminId, id);
        
        Map<String, Object> groupRequest = new HashMap<>();
        if (request.getGroupName() != null) {
            groupRequest.put("groupName", request.getGroupName());
        }
        if (request.getDescription() != null) {
            groupRequest.put("description", request.getDescription());
        }
        if (request.getCriteria() != null) {
            groupRequest.put("criteria", request.getCriteria());
        }
        
        Map<String, Object> group = notificationServiceRestClient.updateUserGroup(id, groupRequest);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_UPDATED,
                "USER_GROUP", id,
                "Updated user group: " + id);
        
        return ResponseEntity.ok(ApiResponse.success("User group updated successfully", group));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete User Group", description = "Delete user group")
    public ResponseEntity<ApiResponse<Void>> deleteUserGroup(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Deleting user group: {}", adminId, id);
        
        notificationServiceRestClient.deleteUserGroup(id);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_DELETED,
                "USER_GROUP", id,
                "Deleted user group: " + id);
        
        return ResponseEntity.ok(ApiResponse.success("User group deleted successfully", null));
    }

    @PostMapping("/{id}/refresh")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Refresh User Group Members", description = "Recalculate member count for user group")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshUserGroupMembers(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        log.info("Admin {}: Refreshing user group members: {}", adminId, id);
        
        Map<String, Object> result = notificationServiceRestClient.refreshUserGroupMembers(id);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.NOTIFICATION_UPDATED,
                "USER_GROUP", id,
                "Refreshed user group members: " + id);
        
        return ResponseEntity.ok(ApiResponse.success("User group members refreshed successfully", result));
    }
}

