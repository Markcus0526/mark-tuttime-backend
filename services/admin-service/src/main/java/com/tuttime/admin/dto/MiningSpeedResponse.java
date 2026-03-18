package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mining Speed Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningSpeedResponse {
    private Long userId;
    private Double baseSpeed; // 1 AP/hour
    private Double referralBonus; // +0.2 AP/hour per active referral
    private Double statusBonus; // +10% to +50% based on status
    private Double stakingBonus; // +10% to +50% based on staking
    private Double serviceActivityBonus; // +50% or +100% for 30 days
    private Double totalSpeed; // Total calculated speed
    private Double maxSpeed; // 5 AP/hour limit
    private Boolean isActive;
}

