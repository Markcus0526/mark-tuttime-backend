package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Fraud Analytics Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudAnalyticsResponse {
    private Long totalFlags;
    private Long unresolvedFlags;
    private Long blockedDevices;
    private Long blockedIps;
    private Long whitelistedDevices;
    private Long whitelistedIps;
    private List<FraudTypeStats> fraudTypeStats;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FraudTypeStats {
        private String fraudType;
        private Long count;
    }
}

