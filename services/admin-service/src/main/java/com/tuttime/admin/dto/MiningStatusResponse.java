package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Mining Status Response DTO
 * Comprehensive mining status including balance, speed breakdown, and session info
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningStatusResponse {
    private Long userId;
    private BigDecimal currentBalance; // Changed from Long to BigDecimal to preserve decimal precision
    private BigDecimal monthlyAccumulated; // Changed from Long to BigDecimal to preserve decimal precision
    private BigDecimal lifetimeAccumulated; // Changed from Long to BigDecimal to preserve decimal precision
    private BigDecimal dailyMinedApBalance; // Changed from Long to BigDecimal to preserve decimal precision
    private Boolean miningActive;
    private MiningSpeedBreakdownResponse speedBreakdown;
    private BigDecimal totalApEarned; // Changed from Long to BigDecimal to preserve decimal precision
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime sessionStartedAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastActivity;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime lastUpdated;
    private Boolean isPremiumAutoMining;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime autoMiningEnabledAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime autoMiningExpiresAt;
}

