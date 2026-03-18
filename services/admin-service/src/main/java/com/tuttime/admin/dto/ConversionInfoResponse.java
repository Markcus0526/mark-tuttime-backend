package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Conversion Info Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionInfoResponse {
    private Long userId;
    private BigDecimal lastMonthApartConversion;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime nextConversionTime;
    private BigDecimal thisMonthForecastAmount;
}

