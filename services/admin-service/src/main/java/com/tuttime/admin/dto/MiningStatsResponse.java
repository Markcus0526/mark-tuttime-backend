package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Mining Stats Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningStatsResponse {
    private BigDecimal totalApCredits;
    private BigDecimal monthlyApCredits;
    private Long activeMiningSessions;
    private Long activeMinerCount;
    private Long inactiveMinerCount;
    private Long neverStartedMiningCount;
    private BigDecimal averageMiningSpeed;
    private OffsetDateTime lastNotificationSentAt; // Deprecated: use lastInactiveNotificationSentAt
    private OffsetDateTime lastInactiveNotificationSentAt; // Last time notification sent to inactive mining users
    private OffsetDateTime lastNeverStartedNotificationSentAt; // Last time notification sent to never-started users
    private List<DailyMiningStats> dailyStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyMiningStats {
        private LocalDate date;
        private BigDecimal apEarned;
        private Long activeSessions;
    }
}

