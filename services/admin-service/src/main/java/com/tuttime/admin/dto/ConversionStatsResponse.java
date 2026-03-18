package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Conversion Stats Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionStatsResponse {
    private BigDecimal totalConverted;
    private BigDecimal lastMonthConverted;
    private BigDecimal thisMonthForecast;
    private LocalDate nextConversionDate;
    private List<MonthlyConversion> monthlyConversions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyConversion {
        private LocalDate month;
        private BigDecimal apCredits;
        private BigDecimal tokens;
        private Long participants;
    }
}

