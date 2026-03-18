package com.tuttime.admin.controller;

import com.tuttime.admin.dto.TaskRequest;
import com.tuttime.admin.dto.TaskResponse;
import com.tuttime.admin.service.AdminTaskService;
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
 * Admin Task Management Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/tasks")
@RequiredArgsConstructor
@Tag(name = "Admin Task Management", description = "Task management APIs for admins")
public class AdminTaskController {

    private final AdminTaskService taskService;
    private final AdminAuditService auditService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "List all tasks", description = "Get paginated list of all tasks with optional status filter")
    public ResponseEntity<ApiResponse<Map<String, Object>>> listTasks(
            @PageableDefault(size = 50) Pageable pageable,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal Long adminId) {
        
        Page<TaskResponse> tasks = taskService.listTasks(pageable, status);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_VIEWED,
                "TASK", null, "Listed all tasks");
        
        // Convert Page to Map to ensure proper JSON serialization
        // Spring's PageImpl serialization can be unstable, so we manually create a Map
        Map<String, Object> pageMap = new java.util.HashMap<>();
        pageMap.put("content", tasks.getContent());
        pageMap.put("totalElements", tasks.getTotalElements());
        pageMap.put("totalPages", tasks.getTotalPages());
        pageMap.put("number", tasks.getNumber());
        pageMap.put("size", tasks.getSize());
        pageMap.put("numberOfElements", tasks.getNumberOfElements());
        pageMap.put("first", tasks.isFirst());
        pageMap.put("last", tasks.isLast());
        pageMap.put("empty", tasks.isEmpty());
        pageMap.put("pageable", Map.of(
            "pageNumber", tasks.getPageable().getPageNumber(),
            "pageSize", tasks.getPageable().getPageSize(),
            "paged", tasks.getPageable().isPaged()
        ));
        
        return ResponseEntity.ok(ApiResponse.success("Tasks retrieved successfully", pageMap));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get task details", description = "Get details of a specific task")
    public ResponseEntity<ApiResponse<TaskResponse>> getTask(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        TaskResponse task = taskService.getTask(id);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_VIEWED,
                "TASK", id, "Viewed task details");
        
        return ResponseEntity.ok(ApiResponse.success("Task retrieved successfully", task));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Create task", description = "Create a new task")
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        TaskResponse task = taskService.createTask(request, adminId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_CREATED,
                "TASK", task.getTaskId(), 
                String.format("Created task: %s", request.getTaskTitle()));
        
        return ResponseEntity.ok(ApiResponse.success("Task created successfully", task));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Update task", description = "Update an existing task")
    public ResponseEntity<ApiResponse<TaskResponse>> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            @AuthenticationPrincipal Long adminId) {
        
        TaskResponse task = taskService.updateTask(id, request);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_UPDATED,
                "TASK", id, "Updated task");
        
        return ResponseEntity.ok(ApiResponse.success("Task updated successfully", task));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete task", description = "Delete a task")
    public ResponseEntity<ApiResponse<String>> deleteTask(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        taskService.deleteTask(id);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_DELETED,
                "TASK", id, "Deleted task");
        
        return ResponseEntity.ok(ApiResponse.success("Task deleted successfully"));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Activate task", description = "Activate a task to make it available to users")
    public ResponseEntity<ApiResponse<TaskResponse>> activateTask(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        TaskResponse task = taskService.activateTask(id);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_ACTIVATED,
                "TASK", id, "Activated task");
        
        return ResponseEntity.ok(ApiResponse.success("Task activated successfully", task));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR')")
    @Operation(summary = "Deactivate task", description = "Deactivate a task to make it unavailable to users")
    public ResponseEntity<ApiResponse<TaskResponse>> deactivateTask(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminId) {
        
        TaskResponse task = taskService.deactivateTask(id);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_DEACTIVATED,
                "TASK", id, "Deactivated task");
        
        return ResponseEntity.ok(ApiResponse.success("Task deactivated successfully", task));
    }

    // Watch Video Task Statistics Endpoints

    @GetMapping("/watch-video/statistics/daily")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get daily watch video task statistics", description = "Get statistics for watch video tasks for a specific date")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getDailyWatchVideoStatistics(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime date,
            @AuthenticationPrincipal Long adminId) {
        
        java.util.Map<String, Object> stats = taskService.getDailyWatchVideoStatistics(date);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_VIEWED,
                "TASK", null, "Viewed daily watch video task statistics");
        
        return ResponseEntity.ok(ApiResponse.success("Daily watch video statistics retrieved successfully", stats));
    }

    @GetMapping("/watch-video/statistics/task/{taskId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get watch video task statistics", description = "Get statistics for a specific watch video task")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getWatchVideoTaskStatistics(
            @PathVariable Long taskId,
            @AuthenticationPrincipal Long adminId) {
        
        java.util.Map<String, Object> stats = taskService.getWatchVideoTaskStatistics(taskId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_VIEWED,
                "TASK", taskId, "Viewed watch video task statistics");
        
        return ResponseEntity.ok(ApiResponse.success("Watch video task statistics retrieved successfully", stats));
    }

    @GetMapping("/watch-video/statistics/user/{userId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get user watch video task statistics", description = "Get statistics for a specific user's watch video tasks")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getUserWatchVideoStatistics(
            @PathVariable Long userId,
            @AuthenticationPrincipal Long adminId) {
        
        java.util.Map<String, Object> stats = taskService.getUserWatchVideoStatistics(userId);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.USER_VIEWED,
                "USER", userId, "Viewed user watch video task statistics");
        
        return ResponseEntity.ok(ApiResponse.success("User watch video statistics retrieved successfully", stats));
    }

    @GetMapping("/watch-video/statistics/range")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'VIEWER')")
    @Operation(summary = "Get date range watch video task statistics", description = "Get statistics for watch video tasks for a date range")
    public ResponseEntity<ApiResponse<java.util.List<java.util.Map<String, Object>>>> getDateRangeWatchVideoStatistics(
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime startDate,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime endDate,
            @AuthenticationPrincipal Long adminId) {
        
        java.util.List<java.util.Map<String, Object>> stats = taskService.getDateRangeWatchVideoStatistics(startDate, endDate);
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_VIEWED,
                "TASK", null, "Viewed date range watch video task statistics");
        
        return ResponseEntity.ok(ApiResponse.success("Date range watch video statistics retrieved successfully", stats));
    }

    @PostMapping("/watch-video/backfill-existing-users")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Backfill existing users with watch video tasks", 
               description = "Initialize watch video tasks for users who were created before this feature was implemented")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> backfillExistingUsers(
            @AuthenticationPrincipal Long adminId) {
        
        java.util.Map<String, Object> result = taskService.backfillExistingUsersWatchVideoTasks();
        auditService.logAction(adminId, null,
                com.tuttime.admin.entity.AdminAuditLog.AdminActionType.TASK_VIEWED,
                "TASK", null, "Backfilled existing users with watch video tasks");
        
        return ResponseEntity.ok(ApiResponse.success("Backfill completed successfully", result));
    }
}

