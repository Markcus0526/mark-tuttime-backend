package com.tuttime.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

/**
 * Restaurant Audit Log Entity
 *
 * <p>Tracks all restaurant actions for security and compliance.
 * Every restaurant action should be logged here.
 */
@Entity
@Table(name = "restaurant_audit_logs", schema = "restaurant_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RestaurantAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "restaurant_id")
    private Long restaurantId;

    @Column(name = "restaurant_name", length = 100)
    private String restaurantName;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private RestaurantActionType actionType;

    @Column(name = "resource_type", length = 50)
    private String resourceType; // e.g., "USER", "MINING", "TOKEN", "STAKING"

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "request_method", length = 10)
    private String requestMethod;

    @Column(name = "request_path", length = 500)
    private String requestPath;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;

    /**
     * Restaurant Action Types
     */
    public enum RestaurantActionType {
        // Authentication
        LOGIN,
        LOGOUT,
        LOGIN_FAILED,
        
        // Menu Management
        USER_VIEWED,
        USER_UPDATED,
        USER_BLOCKED,
        USER_UNBLOCKED,
        USER_DELETED,

        // Status Management
        STATUS_UPDATED,

        // Task Management
        TASK_VIEWED,
        TASK_CREATED,
        TASK_UPDATED,
        TASK_DELETED,
        TASK_ACTIVATED,
        TASK_DEACTIVATED,
        
        // Ad Management
        AD_VIEWED,
        AD_CREATED,
        AD_UPDATED,
        AD_DELETED,
        AD_APPROVED,
        AD_REJECTED,

        // System Configuration
        CONFIG_UPDATED,

        // Language Management
        LANGUAGE_CREATED,
        LANGUAGE_UPDATED,
        LANGUAGE_DELETED,

        // Social Management
        ANALYTICS_VIEWED,

    }
}

