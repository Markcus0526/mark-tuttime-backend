package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Task Completion Statistics Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCompletionStatsResponse {
    private List<DailyTaskStat> dailyStats;
    private List<MonthlyTaskStat> monthlyStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTaskStat {
        private LocalDate date;
        private Long taskId;
        private String taskTitle;
        private Long completionCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyTaskStat {
        private String month; // Format: YYYY-MM
        private Long taskId;
        private String taskTitle;
        private Long completionCount;
    }
}

