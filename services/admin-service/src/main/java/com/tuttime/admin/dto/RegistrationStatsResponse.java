package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Registration Statistics Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistrationStatsResponse {
    private List<DailyRegistrationStat> dailyStats;
    private List<MonthlyRegistrationStat> monthlyStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRegistrationStat {
        private LocalDate date;
        private Long count;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRegistrationStat {
        private String month; // Format: YYYY-MM
        private Long count;
    }
}

