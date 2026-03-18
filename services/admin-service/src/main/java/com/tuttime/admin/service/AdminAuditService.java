package com.tuttime.admin.service;

import com.tuttime.admin.dto.AuditLogResponse;
import com.tuttime.admin.entity.AdminAuditLog;
import com.tuttime.admin.repository.AdminAuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Admin Audit Service
 * 
 * <p>Logs all admin actions for security and compliance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuditService {

    private final AdminAuditLogRepository auditLogRepository;

    /**
     * Log admin action
     */
    @Transactional
    public void logAction(Long adminId, String adminUsername, AdminAuditLog.AdminActionType actionType,
                          String resourceType, Long resourceId, String description) {
        try {
            HttpServletRequest request = getCurrentRequest();
            
            AdminAuditLog auditLog = AdminAuditLog.builder()
                    .adminId(adminId)
                    .adminUsername(adminUsername)
                    .actionType(actionType)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .description(description)
                    .ipAddress(extractIpAddress(request))
                    .userAgent(extractUserAgent(request))
                    .requestMethod(request != null ? request.getMethod() : null)
                    .requestPath(request != null ? request.getRequestURI() : null)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log admin action: adminId={}, actionType={}", adminId, actionType, e);
            // Don't throw - audit logging failure shouldn't break the operation
        }
    }

    /**
     * Log login
     */
    public void logLogin(Long adminId, String username, String ipAddress, String userAgent) {
        logAction(adminId, username, AdminAuditLog.AdminActionType.LOGIN, null, null,
                "Admin login successful");
    }

    /**
     * Log logout
     */
    public void logLogout(Long adminId) {
        logAction(adminId, null, AdminAuditLog.AdminActionType.LOGOUT, null, null,
                "Admin logout");
    }

    /**
     * Log failed login
     */
    public void logLoginFailed(String username, String ipAddress, String userAgent, String reason) {
        try {
            AdminAuditLog auditLog = AdminAuditLog.builder()
                    .adminId(null) // Unknown admin
                    .adminUsername(username)
                    .actionType(AdminAuditLog.AdminActionType.LOGIN_FAILED)
                    .description("Login failed: " + reason)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log failed login: username={}", username, e);
        }
    }

    /**
     * Get current HTTP request
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * Extract IP address from request
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    /**
     * Extract user agent from request
     */
    private String extractUserAgent(HttpServletRequest request) {
        return request != null ? request.getHeader("User-Agent") : null;
    }

    /**
     * Get audit logs with filters
     * 
     * @param adminId Optional admin ID filter
     * @param actionType Optional action type filter
     * @param resourceType Optional resource type filter
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            Long adminId,
            AdminAuditLog.AdminActionType actionType,
            String resourceType,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Pageable pageable) {
        
        Page<AdminAuditLog> logs;
        
        if (adminId != null && startDate != null && endDate != null) {
            // Filter by admin and date range
            logs = auditLogRepository.findByAdminAndDateRange(adminId, startDate, endDate, pageable);
        } else if (startDate != null && endDate != null) {
            // Filter by date range only
            logs = auditLogRepository.findByDateRange(startDate, endDate, pageable);
        } else if (adminId != null) {
            // Filter by admin only
            logs = auditLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId, pageable);
        } else if (actionType != null) {
            // Filter by action type only
            logs = auditLogRepository.findByActionTypeOrderByCreatedAtDesc(actionType, pageable);
        } else {
            // No filters - get all logs
            logs = auditLogRepository.findAll(pageable);
        }
        
        // Apply resource type filter if specified
        // Note: For better performance, this should be added as a repository method
        if (resourceType != null && !resourceType.isEmpty()) {
            // Filter content and create new page
            java.util.List<AdminAuditLog> filteredContent = logs.getContent().stream()
                    .filter(log -> resourceType.equals(log.getResourceType()))
                    .collect(java.util.stream.Collectors.toList());
            
            logs = new org.springframework.data.domain.PageImpl<>(
                    filteredContent, 
                    pageable, 
                    filteredContent.size());
        }
        
        // Convert to response DTOs
        return logs.map(this::convertToResponse);
    }

    /**
     * Convert AdminAuditLog entity to AuditLogResponse DTO
     */
    private AuditLogResponse convertToResponse(AdminAuditLog log) {
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .adminId(log.getAdminId())
                .adminUsername(log.getAdminUsername())
                .actionType(log.getActionType())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .requestMethod(log.getRequestMethod())
                .requestPath(log.getRequestPath())
                .requestBody(log.getRequestBody())
                .responseStatus(log.getResponseStatus())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

