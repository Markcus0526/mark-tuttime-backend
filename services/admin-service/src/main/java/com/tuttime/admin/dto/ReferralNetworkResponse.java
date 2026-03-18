package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Referral Network Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReferralNetworkResponse {
    private Long userId;
    private String username;
    private Integer totalReferrals;
    private Integer activeReferrals;
    private List<ReferralNode> referralTree;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReferralNode {
        private Long userId;
        private String username;
        private Boolean isActive;
        private Integer referralCount;
        private java.math.BigDecimal totalMiningSpeed; // Current total mining speed in AP/hour
        private List<ReferralNode> children;
    }
}

