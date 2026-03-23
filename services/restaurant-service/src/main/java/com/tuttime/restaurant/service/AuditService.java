package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.AuditLogResponse;
import com.tuttime.restaurant.entity.RestaurantAuditLog;
import com.tuttime.restaurant.repository.RestaurantAuditLogRepository;
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
 * Restaurant Audit Service
 * 
 * <p>Logs all restaurant actions for security and compliance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final RestaurantAuditLogRepository auditLogRepository;

    /**
     * Log restaurant action
     */
    @Transactional
    public void logAction(Long restaurantId, String name, RestaurantAuditLog.RestaurantActionType actionType,
                          String resourceType, Long resourceId, String description) {
        try {
            HttpServletRequest request = getCurrentRequest();

            RestaurantAuditLog auditLog = RestaurantAuditLog.builder()
                    .restaurantId(restaurantId)
                    .restaurantName(name)
                    .actionType(actionType)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .description(description)
                    .ipAddress(extractIpAddress(request))
                    .requestMethod(request != null ? request.getMethod() : null)
                    .requestPath(request != null ? request.getRequestURI() : null)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to log restaurant action: restaurantId={}, actionType={}", restaurantId, actionType, e);
            // Don't throw - audit logging failure shouldn't break the operation
        }
    }

    /**
     * Log login
     */
    public void logLogin(Long restaurantId, String username, String ipAddress) {
        logAction(restaurantId, username, RestaurantAuditLog.RestaurantActionType.LOGIN, null, null,
                "Restaurant login successful");
    }

    /**
     * Log logout
     */
    public void logLogout(Long restaurantId) {
        logAction(restaurantId, null, RestaurantAuditLog.RestaurantActionType.LOGOUT, null, null,
                "Restaurant logout");
    }

    /**
     * Log failed login
     */
    public void logLoginFailed(String username, String ipAddress, String reason) {
        try {
            RestaurantAuditLog auditLog = RestaurantAuditLog.builder()
                    .resourceId(null) // Unknown restaurant
                    .restaurantName(username)
                    .actionType(RestaurantAuditLog.RestaurantActionType.LOGIN_FAILED)
                    .description("Login failed: " + reason)
                    .ipAddress(ipAddress)
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
     * @param restaurantId Optional restaurant ID filter
     * @param actionType Optional action type filter
     * @param resourceType Optional resource type filter
     * @param startDate Optional start date filter
     * @param endDate Optional end date filter
     * @param pageable Pagination parameters
     * @return Page of audit logs
     */
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(
            Long restaurantId,
            RestaurantAuditLog.RestaurantActionType actionType,
            String resourceType,
            OffsetDateTime startDate,
            OffsetDateTime endDate,
            Pageable pageable) {
        
        Page<RestaurantAuditLog> logs;
        
        if (restaurantId != null && startDate != null && endDate != null) {
            // Filter by restaurant and date range
            logs = auditLogRepository.findByRestaurantAndDateRange(restaurantId, startDate, endDate, pageable);
        } else if (startDate != null && endDate != null) {
            // Filter by date range only
            logs = auditLogRepository.findByDateRange(startDate, endDate, pageable);
        } else if (restaurantId != null) {
            // Filter by restaurant only
            logs = auditLogRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable);
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
            java.util.List<RestaurantAuditLog> filteredContent = logs.getContent().stream()
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
     * Convert RestaurantAuditLog entity to AuditLogResponse DTO
     */
    private AuditLogResponse convertToResponse(RestaurantAuditLog log) {
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .restaurantId(log.getRestaurantId())
                .name(log.getRestaurantName())
                .actionType(log.getActionType())
                .resourceType(log.getResourceType())
                .resourceId(log.getResourceId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .requestMethod(log.getRequestMethod())
                .requestPath(log.getRequestPath())
                .requestBody(log.getRequestBody())
                .responseStatus(log.getResponseStatus())
                .createdAt(log.getCreatedAt())
                .build();
    }
}

