package com.tuttime.admin.controller;

import com.tuttime.admin.dto.SystemLogResponse;
import com.tuttime.admin.service.AdminLogService;
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
import java.util.List;

/**
 * Admin System Logs Controller
 * 
 * <p>Provides endpoints for viewing system logs from all services:
 * <ul>
 *   <li>View system logs from all services</li>
 *   <li>Filter by service, log level, date range</li>
 *   <li>View error logs only</li>
 *   <li>View warning logs only</li>
 *   <li>List all services</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/observations/logs")
@RequiredArgsConstructor
@Tag(name = "Admin System Logs", description = "System logs viewing and filtering APIs")
public class AdminLogController {

    private final AdminLogService logService;
    private final AdminAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get system logs", 
               description = "Retrieves system logs with optional filters: service, level, startDate, endDate")
    public ResponseEntity<ApiResponse<Page<SystemLogResponse>>> getLogs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<SystemLogResponse> logs = logService.getLogs(service, level, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "SYSTEM_LOG", null, "Viewed system logs");
        
        return ResponseEntity.ok(ApiResponse.success("System logs retrieved successfully", logs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get specific log entry", 
               description = "Retrieves a specific log entry by ID")
    public ResponseEntity<ApiResponse<SystemLogResponse>> getLogById(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        SystemLogResponse log = logService.getLogById(id);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "SYSTEM_LOG", id, "Viewed log entry");
        
        return ResponseEntity.ok(ApiResponse.success("Log entry retrieved successfully", log));
    }

    @GetMapping("/services")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List all services", 
               description = "Retrieves a list of all services that have logs")
    public ResponseEntity<ApiResponse<List<String>>> getAllServices(
            @AuthenticationPrincipal Long adminId) {
        
        List<String> services = logService.getAllServices();
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "SYSTEM_LOG", null, "Viewed service list");
        
        return ResponseEntity.ok(ApiResponse.success("Services retrieved successfully", services));
    }

    @GetMapping("/errors")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get error logs only", 
               description = "Retrieves only error and fatal level logs with optional filters")
    public ResponseEntity<ApiResponse<Page<SystemLogResponse>>> getErrorLogs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<SystemLogResponse> logs = logService.getErrorLogs(service, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "SYSTEM_LOG", null, "Viewed error logs");
        
        return ResponseEntity.ok(ApiResponse.success("Error logs retrieved successfully", logs));
    }

    @GetMapping("/warnings")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get warning logs only", 
               description = "Retrieves only warning level logs with optional filters")
    public ResponseEntity<ApiResponse<Page<SystemLogResponse>>> getWarningLogs(
            @RequestParam(required = false) String service,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable,
            @AuthenticationPrincipal Long adminId) {
        
        Page<SystemLogResponse> logs = logService.getWarningLogs(service, startDate, endDate, pageable);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.CONFIG_UPDATED,
                "SYSTEM_LOG", null, "Viewed warning logs");
        
        return ResponseEntity.ok(ApiResponse.success("Warning logs retrieved successfully", logs));
    }
}

