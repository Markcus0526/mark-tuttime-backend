package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * User Overview DTO for Admin Portal
 * 
 * <p>Comprehensive user profile overview containing all relevant information
 * for admin user management portal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOverview {
    private Long userId;
    private UserBasicInfo basicInfo;
    private UserBalances balances;
    private UserMiningInfo mining;
    private UserReferralInfo referrals;
    private UserStakingInfo staking;
    private UserHistory history;
    private AccountStatus accountStatus;
}

