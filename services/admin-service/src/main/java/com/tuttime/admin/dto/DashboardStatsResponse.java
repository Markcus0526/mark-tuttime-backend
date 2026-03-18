package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Dashboard Stats Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalUsers;
    private Long activeUsers;
    private Long blockedUsers;
    private BigDecimal totalApCredits;
    private BigDecimal totalApartTokens;
    private BigDecimal totalStaked;
    private Long totalReferrals;
    private Long activeMiningSessions;
    
    // New fields for enhanced dashboard
    private Map<String, Long> usersByLevel;  // BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, ELITE
    private Long activeAutoMinings;  // Count of users with active auto-mining
    private ConversionSummaryResponse lastConversion;  // Last conversion statistics
    private BigDecimal totalStakingApy;  // Weighted average APY
    private BigDecimal totalRewardsClaimed;  // Total rewards claimed from staking
    private BigDecimal tvl;  // Total Value Locked (total staked amount)
}

