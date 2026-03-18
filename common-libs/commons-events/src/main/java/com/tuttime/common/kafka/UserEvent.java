package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Kafka event for user-related events
 * Published by User Service, consumed by other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {

    public enum UserEventType {
        USER_CREATED,
        USER_UPDATED,
        USER_VERIFIED,
        USER_DEACTIVATED,
        USER_BLOCKED,
        USER_UNBLOCKED,
        KYC_COMPLETED,
        KYC_REJECTED,
        EMAIL_VERIFIED,
        PASSWORD_CHANGED
    }

    @JsonProperty("event_type")
    private UserEventType eventType;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("username")
    private String username;

    @JsonProperty("is_verified")
    private Boolean isVerified;

    @JsonProperty("kyc_completed")
    private Boolean kycCompleted;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_blocked")
    private Boolean isBlocked;

    @JsonProperty("status_level")
    private String statusLevel; // BRONZE, GOLD, PLATINUM, etc.

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime timestamp;

    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0.0"; // Event schema version for evolution support
}

