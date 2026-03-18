package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Kafka event for conversion-related events
 * Published by Conversion Service, consumed by other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionEvent {

    public enum ConversionEventType {
        CONVERSION_INITIATED,
        CONVERSION_COMPLETED,
        CONVERSION_FAILED
    }

    @JsonProperty("event_type")
    private ConversionEventType eventType;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("task_id")
    private Long taskId;

    @JsonProperty("conversion_month")
    private LocalDate conversionMonth;

    @JsonProperty("ap_credits_amount")
    private BigDecimal apCreditsAmount;

    @JsonProperty("apart_tokens_amount")
    private BigDecimal apartTokensAmount;

    @JsonProperty("conversion_rate")
    private BigDecimal conversionRate;

    @JsonProperty("status")
    private String status; // PENDING, PROCESSING, COMPLETED, FAILED

    @JsonProperty("failure_reason")
    private String failureReason;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    @JsonProperty("timestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime timestamp;

    @JsonProperty("version")
    @Builder.Default
    private String version = "1.0.0"; // Event schema version for evolution support
}

