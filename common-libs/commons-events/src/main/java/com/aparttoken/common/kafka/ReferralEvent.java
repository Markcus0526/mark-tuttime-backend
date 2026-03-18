package com.aparttoken.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Kafka event for referral-related events
 * Published by User Service during signup, consumed by Rewards Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralEvent {

    public enum ReferralEventType {
        REFERRAL_CODE_SUBMITTED
    }

    @JsonProperty("event_type")
    private ReferralEventType eventType;

    @JsonProperty("referee_id")
    private Long refereeId; // The user who was referred (new user)

    @JsonProperty("referral_code")
    private String referralCode; // The referral code provided during signup

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime timestamp;

    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0.0"; // Event schema version for evolution support
}

