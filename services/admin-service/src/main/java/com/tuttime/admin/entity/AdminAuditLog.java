package com.tuttime.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

/**
 * Admin Audit Log Entity
 *
 * <p>Tracks all admin actions for security and compliance.
 * Every admin action should be logged here.
 */
@Entity
@Table(name = "admin_audit_logs", schema = "admin_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "admin_username", length = 100)
    private String adminUsername;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private AdminActionType actionType;

    @Column(name = "resource_type", length = 50)
    private String resourceType; // e.g., "USER", "MINING", "TOKEN", "STAKING"

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

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
     * Admin Action Types
     */
    public enum AdminActionType {
        // Authentication
        LOGIN,
        LOGOUT,
        LOGIN_FAILED,
        
        // User Management
        USER_VIEWED,
        USER_UPDATED,
        USER_BLOCKED,
        USER_UNBLOCKED,
        USER_DELETED,
        USER_KYC_APPROVED,
        USER_KYC_REJECTED,
        
        // Mining Management
        MINING_BALANCE_ADJUSTED,
        MINING_SESSION_VIEWED,
        MINING_SPEED_ADJUSTED,
        
        // Token Management
        TOKEN_BALANCE_ADJUSTED,
        TOKEN_TRANSACTION_VIEWED,
        TOKEN_BURNED,
        
        // Staking Management
        STAKING_VIEWED,
        STAKING_ADJUSTED,
        
        // Referral Management
        REFERRAL_VIEWED,
        REFERRAL_CREATED,
        REFERRAL_ADJUSTED,
        
        // Status Management
        STATUS_UPDATED,
        
        // Conversion Management
        CONVERSION_VIEWED,
        CONVERSION_TRIGGERED,
        
        // Notification Management
        NOTIFICATION_VIEWED,
        NOTIFICATION_CREATED,
        NOTIFICATION_UPDATED,
        NOTIFICATION_DELETED,
        NOTIFICATION_PUBLISHED,
        NOTIFICATION_CANCELLED,
        NOTIFICATION_SCHEDULED,
        
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
        
        // Vesting Pool Management
        VESTING_POOL_VIEWED,
        VESTING_POOL_CREATED,
        VESTING_POOL_UPDATED,
        VESTING_POOL_BALANCE_VIEWED,
        VESTING_POOL_FUNDED,
        VESTING_INVESTOR_VIEWED,
        VESTING_INVESTOR_ADDED,
        VESTING_CLAIM_STATUS_VIEWED,
        
        // Video Management
        VIDEO_VIEWED,
        VIDEO_CREATED,
        VIDEO_UPDATED,
        VIDEO_DELETED,
        VIDEO_APPROVED,
        VIDEO_REJECTED,
        VIDEO_PUBLISHED,
        VIDEO_SCHEDULED,
        
        // Boost View
        BOOST_VIEWED,
        
        // Anti-Fraud - Whitelisting
        DEVICE_WHITELISTED,
        IP_WHITELISTED,
        FRAUD_FLAG_VIEWED,
        
        // User Blocklist
        USER_BLOCKLIST_VIEWED,
        
        // Token Address Blocklist/Whitelist
        ADDRESS_BLOCKLIST_VIEWED,
        ADDRESS_WHITELIST_VIEWED,
        ADDRESS_BLOCKED,
        ADDRESS_UNBLOCKED,
        ADDRESS_WHITELISTED,
        ADDRESS_WHITELIST_REMOVED,
        
        // Anti-Fraud
        FRAUD_FLAG_ADDED,
        FRAUD_FLAG_REMOVED,
        DEVICE_BLOCKED,
        IP_BLOCKED,
        
        // Content/Map Management
        MAP_MARKER_CREATED,
        MAP_MARKER_UPDATED,
        MAP_MARKER_DELETED,
        MINER_RATING_UPDATED,
        
        // Token Whitelist/Blacklist
        TOKEN_WHITELISTED,
        TOKEN_BLACKLISTED,
        
        // System Configuration
        CONFIG_UPDATED,
        
        // Token Reserve Observation
        TOKEN_RESERVE_VIEWED,
        TOKEN_RESERVE_ALARM_CREATED,
        TOKEN_RESERVE_ALARM_UPDATED,
        TOKEN_RESERVE_ALARM_DELETED,
        
        // Language Management
        LANGUAGE_CREATED,
        LANGUAGE_UPDATED,
        LANGUAGE_DELETED,
        TRANSLATION_UPLOADED,
        TRANSLATION_UPDATED,
        
        // Staking Tier Management
        STAKING_TIER_CREATED,
        STAKING_TIER_UPDATED,
        STAKING_TIER_DELETED,
        STAKING_TIERS_REORDERED,
        
        // Status Level Management
        STATUS_LEVEL_CREATED,
        STATUS_LEVEL_UPDATED,
        STATUS_LEVEL_DELETED,
        
        // Social Management
        POST_VIEWED,
        POST_DELETED,
        COMMENT_VIEWED,
        COMMENT_DELETED,
        HASHTAG_VIEWED,
        USER_BLOCK_VIEWED,
        PRIVACY_SETTINGS_VIEWED,
        PRIVACY_SETTINGS_UPDATED,
        ANALYTICS_VIEWED,
        
        // Admin User Management
        ADMIN_USER_VIEWED,
        ADMIN_USER_CREATED,
        ADMIN_USER_UPDATED,
        ADMIN_USER_DELETED,
        ADMIN_USER_ACTIVATED,
        ADMIN_USER_DEACTIVATED,
        ADMIN_USER_UNLOCKED,
        ADMIN_USER_PASSWORD_CHANGED,
        ADMIN_USER_PASSWORD_RESET,
        ADMIN_USER_LOGIN_ATTEMPTS_RESET,
        
        // Other
        EXPORT_DATA,
        BULK_OPERATION
    }
}

