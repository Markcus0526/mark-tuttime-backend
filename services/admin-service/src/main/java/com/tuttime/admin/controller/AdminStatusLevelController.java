package com.tuttime.admin.controller;

import com.tuttime.admin.dto.StatusLevelRequest;
import com.tuttime.admin.dto.StatusLevelResponse;
import com.tuttime.admin.entity.AdminAuditLog;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminSystemConfigService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin Status Level Management Controller
 * 
 * <p>Provides endpoints for managing user status levels:
 * <ul>
 *   <li>List/create/update/delete status levels</li>
 *   <li>Configure AP boost and pro auto-mining eligibility</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orchestration/status-levels")
@RequiredArgsConstructor
@Tag(name = "Admin Status Level Management", description = "Status level management APIs")
public class AdminStatusLevelController {

    private final AdminSystemConfigService systemConfigService;
    private final AdminAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get all status levels", description = "Get list of all status levels ordered by display order")
    public ResponseEntity<ApiResponse<List<StatusLevelResponse>>> getAllStatusLevels(
            @AuthenticationPrincipal Long adminId) {
        
        List<StatusLevelResponse> levels = systemConfigService.getAllStatusLevels();
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "STATUS_LEVELS", null, "Listed status levels");
        
        return ResponseEntity.ok(ApiResponse.success("Status levels retrieved successfully", levels));
    }

    @GetMapping("/{level}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get status level by name", description = "Get status level details by level name (e.g., SILVER, GOLD)")
    public ResponseEntity<ApiResponse<StatusLevelResponse>> getStatusLevel(
            @PathVariable String level,
            @AuthenticationPrincipal Long adminId) {
        
        StatusLevelResponse statusLevel = systemConfigService.getStatusLevelByLevelName(level);
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "STATUS_LEVELS", null, "Viewed status level: " + level);
        
        return ResponseEntity.ok(ApiResponse.success("Status level retrieved successfully", statusLevel));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Create status level", description = "Create a new status level")
    public ResponseEntity<ApiResponse<StatusLevelResponse>> createStatusLevel(
            @Valid @RequestBody StatusLevelRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        StatusLevelResponse statusLevel = systemConfigService.createStatusLevel(request, adminId);
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.STATUS_LEVEL_CREATED,
                "STATUS_LEVELS", statusLevel.getLevelId(), "Created status level: " + request.getLevelName());
        
        return ResponseEntity.ok(ApiResponse.success("Status level created successfully", statusLevel));
    }

    @PutMapping("/{level}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Update status level", description = "Update an existing status level")
    public ResponseEntity<ApiResponse<StatusLevelResponse>> updateStatusLevel(
            @PathVariable String level,
            @Valid @RequestBody StatusLevelRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        StatusLevelResponse statusLevel = systemConfigService.updateStatusLevel(level, request);
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.STATUS_LEVEL_UPDATED,
                "STATUS_LEVELS", statusLevel.getLevelId(), "Updated status level: " + level);
        
        return ResponseEntity.ok(ApiResponse.success("Status level updated successfully", statusLevel));
    }

    @DeleteMapping("/{level}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete status level", description = "Delete a status level")
    public ResponseEntity<ApiResponse<Void>> deleteStatusLevel(
            @PathVariable String level,
            @AuthenticationPrincipal Long adminId) {
        
        systemConfigService.deleteStatusLevel(level);
        
        auditService.logAction(adminId, null,
                AdminAuditLog.AdminActionType.STATUS_LEVEL_DELETED,
                "STATUS_LEVELS", null, "Deleted status level: " + level);
        
        return ResponseEntity.ok(ApiResponse.success("Status level deleted successfully", null));
    }
}

