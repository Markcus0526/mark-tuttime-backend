package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Kafka event for mining-related events
 * Published by Mining Service, consumed by other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningEvent {

    public enum MiningEventType {
        MINING_STARTED,
        MINING_STOPPED,
        AP_CREDITED,
        AP_BALANCE_UPDATED,
        MINING_SPEED_CHANGED
    }

    @JsonProperty("event_type")
    private MiningEventType eventType;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("ap_amount")
    private BigDecimal apAmount;

    @JsonProperty("current_balance")
    private BigDecimal currentBalance;

    @JsonProperty("monthly_accumulated")
    private BigDecimal monthlyAccumulated;

    @JsonProperty("mining_speed")
    private BigDecimal miningSpeed; // AP Credits per hour

    @JsonProperty("reference_type")
    private String referenceType; // MINING_SESSION, REWARD, etc.

    @JsonProperty("reference_id")
    private Long referenceId;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime timestamp;

    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0.0"; // Event schema version for evolution support
}

