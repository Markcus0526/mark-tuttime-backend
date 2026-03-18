package com.aparttoken.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Kafka event for user blocking/unblocking events.
 * 
 * <p>Published by any service when a user blocks or unblocks another user.
 * Consumed by multiple services (user-service, social-service, etc.) to sync blocking state.
 * 
 * <p>This is a common event that allows any service to publish blocking events
 * and multiple services to consume and process them independently.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserBlockEvent {

    public enum BlockEventType {
        USER_BLOCKED,
        USER_UNBLOCKED
    }

    /**
     * Type of block event (BLOCKED or UNBLOCKED)
     */
    @JsonProperty("event_type")
    private BlockEventType eventType;

    /**
     * User ID who is performing the block/unblock action
     */
    @JsonProperty("blocker_user_id")
    private Long blockerUserId;

    /**
     * User ID who is being blocked/unblocked
     */
    @JsonProperty("blocked_user_id")
    private Long blockedUserId;

    /**
     * Source service that published this event (e.g., "user-service", "social-service")
     */
    @JsonProperty("source_service")
    private String sourceService;

    /**
     * Block context/reason (optional)
     * Examples: "MAP", "SOCIAL", "ADMIN", etc.
     */
    @JsonProperty("block_context")
    private String blockContext;

    /**
     * Additional metadata for the block event
     */
    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    /**
     * Event timestamp (UTC)
     */
    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime timestamp;

    /**
     * Event schema version for evolution support
     */
    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0.0";
}

