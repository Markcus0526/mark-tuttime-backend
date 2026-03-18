package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * DTO for conversion information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionInfo {
    /** Last month APART conversion amount */
    private BigDecimal lastMonthApartConversion;
    
    /** Next conversion time */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime nextConversionTime;
    
    /** This month forecast amount (estimated tokens from conversion) */
    private BigDecimal thisMonthForecastAmount;

    /**
     * Creates an empty ConversionInfo with zero values
     */
    public static ConversionInfo empty() {
        return ConversionInfo.builder()
                .lastMonthApartConversion(BigDecimal.ZERO)
                .nextConversionTime(null)
                .thisMonthForecastAmount(BigDecimal.ZERO)
                .build();
    }
}

