package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Response DTO for user token holding status
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserTokenHoldingStatus {
    /** Current APART balance */
    private BigDecimal currentApartBalance;

    /** This month forecast amount (estimated tokens from conversion) */
    private BigDecimal thisMonthForecastAmount;

    /** Current level (based on token holdings) */
    private String currentLevel;

    /** APARTs needed for next level */
    private BigDecimal apartsForNextLevel;

    /** Next conversion time */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime nextConversionTime;

    /** Last month APART conversion amount */
    private BigDecimal lastMonthApartConversion;
}

