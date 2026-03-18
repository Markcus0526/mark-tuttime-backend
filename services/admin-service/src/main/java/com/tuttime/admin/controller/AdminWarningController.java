package com.tuttime.admin.controller;

import com.tuttime.admin.dto.WarningCategory;
import com.tuttime.admin.dto.WarningResponse;
import com.tuttime.admin.dto.WarningSeverity;
import com.tuttime.admin.service.AdminAuditService;
import com.tuttime.admin.service.AdminWarningService;
import com.tuttime.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
 * Admin Warning Management Controller
 * 
 * <p>Provides endpoints for viewing consolidated warnings from various sources:
 * <ul>
 *   <li>Fraud flags</li>
 *   <li>Brute-force attempts</li>
 *   <li>System stability warnings</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/observations/warnings")
@RequiredArgsConstructor
@Tag(name = "Admin Warning Management", description = "Warning management APIs for security and system stability")
public class AdminWarningController {

    private final AdminWarningService warningService;
    private final AdminAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(
            summary = "Get consolidated warnings",
            description = "Get paginated list of all warnings from various sources (fraud, brute-force, system stability)"
    )
    public ResponseEntity<ApiResponse<Page<WarningResponse>>> getConsolidatedWarnings(
            @PageableDefault(size = 50) Pageable pageable,
            @Parameter(description = "Filter by warning category") @RequestParam(required = false) WarningCategory category,
            @Parameter(description = "Filter by warning severity") @RequestParam(required = false) WarningSeverity severity,
            @Parameter(description = "Filter by resolved status") @RequestParam(required = false) Boolean resolved,
            @Parameter(description = "Start date filter (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @Parameter(description = "End date filter (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @AuthenticationPrincipal Long adminId) {
        
        Page<WarningResponse> warnings = warningService.getConsolidatedWarnings(
                pageable, category, severity, resolved, startDate, endDate);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
                "WARNINGS", null, "Viewed consolidated warnings");
        
        return ResponseEntity.ok(ApiResponse.success("Warnings retrieved successfully", warnings));
    }

    @GetMapping("/brute-force")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(
            summary = "Get brute-force warnings",
            description = "Get paginated list of brute-force attack warnings"
    )
    public ResponseEntity<ApiResponse<Page<WarningResponse>>> getBruteForceWarnings(
            @PageableDefault(size = 50) Pageable pageable,
            @Parameter(description = "Start date filter (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @Parameter(description = "End date filter (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @AuthenticationPrincipal Long adminId) {
        
        Page<WarningResponse> warnings = warningService.getBruteForceWarnings(
                pageable, startDate, endDate);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
                "WARNINGS", null, "Viewed brute-force warnings");
        
        return ResponseEntity.ok(ApiResponse.success("Brute-force warnings retrieved successfully", warnings));
    }

    @GetMapping("/system-stability")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(
            summary = "Get system stability warnings",
            description = "Get paginated list of system stability warnings"
    )
    public ResponseEntity<ApiResponse<Page<WarningResponse>>> getSystemStabilityWarnings(
            @PageableDefault(size = 50) Pageable pageable,
            @Parameter(description = "Start date filter (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @Parameter(description = "End date filter (ISO format)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @AuthenticationPrincipal Long adminId) {
        
        Page<WarningResponse> warnings = warningService.getSystemStabilityWarnings(
                pageable, startDate, endDate);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_VIEWED,
                "WARNINGS", null, "Viewed system stability warnings");
        
        return ResponseEntity.ok(ApiResponse.success("System stability warnings retrieved successfully", warnings));
    }

    @PostMapping("/{warningId}/resolve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(
            summary = "Resolve warning",
            description = "Mark a warning as resolved"
    )
    public ResponseEntity<ApiResponse<Void>> resolveWarning(
            @PathVariable String warningId,
            @AuthenticationPrincipal Long adminId) {
        
        warningService.resolveWarning(warningId);
        
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.FRAUD_FLAG_REMOVED,
                "WARNINGS", null, "Resolved warning: " + warningId);
        
        return ResponseEntity.ok(ApiResponse.success("Warning resolved successfully", null));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(
            summary = "Get warning statistics",
            description = "Get warning statistics by category and severity"
    )
    public ResponseEntity<ApiResponse<com.tuttime.admin.dto.WarningStatisticsResponse>> getWarningStatistics(
            @AuthenticationPrincipal Long adminId) {
        
        com.tuttime.admin.dto.WarningStatisticsResponse statistics = warningService.getWarningStatistics();
        
        return ResponseEntity.ok(ApiResponse.success("Warning statistics retrieved successfully", statistics));
    }
}

