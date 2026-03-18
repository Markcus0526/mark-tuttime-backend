package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * User referral information DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReferralInfo {
    private String referralCode;
    private Integer totalReferrals;
    private Integer activeReferrals;
    private BigDecimal totalEarned;
    private Integer referralLevel;
}

