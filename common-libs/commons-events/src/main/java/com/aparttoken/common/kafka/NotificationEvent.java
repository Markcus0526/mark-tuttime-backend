package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Kafka event for notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {
    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("notification_type")
    private NotificationType notificationType;

    @JsonProperty("channel")
    private NotificationChannel channel; // EMAIL, SMS, PUSH, IN_APP

    @JsonProperty("subject")
    private String subject;

    @JsonProperty("message")
    private String message;

    @JsonProperty("template_id")
    private String templateId; // Optional template identifier
    
    @JsonProperty("template_key")
    private String templateKey; // Optional template key (e.g., 'mining_session_expired')

    @JsonProperty("template_variables")
    private Map<String, String> templateVariables; // Variables for template rendering

    @JsonProperty("priority")
    private NotificationPriority priority; // LOW, NORMAL, HIGH, URGENT

    @JsonProperty("reference_type")
    private String referenceType; // TRANSACTION, REWARD, STAKING, etc.

    @JsonProperty("reference_id")
    private String referenceId;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("scheduled_at")
    private Long scheduledAt; // Optional: schedule notification for future

    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0.0"; // Event schema version for evolution support

    public enum NotificationType {
        USER_REGISTERED,
        USER_VERIFIED,
        KYC_COMPLETED,
        KYC_REJECTED,
        TOKEN_DEPOSIT,
        TOKEN_WITHDRAWAL,
        TOKEN_TRANSFER,
        TRANSACTION_CONFIRMED,
        TRANSACTION_FAILED,
        STAKING_STARTED,
        STAKING_COMPLETED,
        STAKING_REWARD,
        MINING_STARTED,
        MINING_STOPPED,
        AP_CREDITED,
        CONVERSION_COMPLETED,
        CONVERSION_FAILED,
        REFERRAL_BONUS,
        ACTIVE_FRIEND_REWARD,
        BACKLOG_COMPLETED,
        APP_DOWNLOAD_REWARD,
        VIDEO_VIEW_REWARD,
        AD_VIEW_REWARD,
        BOOKING_BOOST_ACTIVATED,
        WITHDRAWAL_REQUESTED,
        WITHDRAWAL_PROCESSING,
        WITHDRAWAL_COMPLETED,
        WITHDRAWAL_FAILED,
        DEPOSIT_CONFIRMED,
        DEPOSIT_FAILED,
        SECURITY_ALERT,
        PASSWORD_CHANGED,
        LOGIN_ATTEMPT_FAILED,
        ACCOUNT_BLOCKED,
        ACCOUNT_UNBLOCKED,
        // Social Media Notifications
        USER_FOLLOWED,
        USER_UNFOLLOWED,
        FOLLOW_REQUEST,
        FOLLOW_REQUEST_APPROVED,
        NEW_POST,
        POST_LIKED,
        POST_UNLIKED,
        POST_COMMENTED,
        COMMENT_LIKED,
        COMMENT_UNLIKED,
        POST_MENTIONED,
        COMMENT_MENTIONED,
        // Messaging Notifications
        NEW_MESSAGE
    }

    public enum NotificationChannel {
        EMAIL,
        SMS,
        PUSH,
        IN_APP
    }

    public enum NotificationPriority {
        LOW,
        NORMAL,
        HIGH,
        URGENT
    }
}

