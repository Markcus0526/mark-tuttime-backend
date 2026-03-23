package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Kafka event for user-related events
 * Published by User Service, consumed by other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantEvent {

    public enum RestaurantEventType {
        RESTAURANT_CREATED,
        RESTAURANT_UPDATED,
        RESTAURANT_VERIFIED,
        RESTAURANT_DEACTIVATED,
        RESTAURANT_BLOCKED,
        RESTAURANT_UNBLOCKED,
        EMAIL_VERIFIED,
        PASSWORD_CHANGED
    }

    @JsonProperty("event_type")
    private RestaurantEventType eventType;

    @JsonProperty("restaurant_id")
    private Long restaurantId;

    @JsonProperty("email")
    private String email;

    @JsonProperty("name")
    private String name;

    @JsonProperty("is_verified")
    private Boolean isVerified;

    @JsonProperty("is_active")
    private Boolean isActive;

    @JsonProperty("is_blocked")
    private Boolean isBlocked;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime timestamp;

    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0.0"; // Event schema version for evolution support
}

