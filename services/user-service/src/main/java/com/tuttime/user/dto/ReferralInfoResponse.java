package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Referral info response DTO from rewards service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralInfoResponse {
    private String referralCode;
    private Integer totalReferrals;
    private Integer activeReferrals;
    private BigDecimal totalEarned;

    public static ReferralInfoResponse empty() {
        return ReferralInfoResponse.builder()
                .referralCode("")
                .totalReferrals(0)
                .activeReferrals(0)
                .totalEarned(BigDecimal.ZERO)
                .build();
    }
}

