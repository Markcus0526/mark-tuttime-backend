package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mining Speed Breakdown Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningSpeedBreakdownResponse {
    private Double totalSpeed;
    private Double baseSpeed;
    private Double friendBonus;
    private Double bookingBonus;
    private Double rentalBonus;
    private Double videoBoost;
    private Double taskBonus;
    private Double statusBonus;
    private Double stakingBonus;
    private Double streakBonus;
    private Boolean isCapped;
}

