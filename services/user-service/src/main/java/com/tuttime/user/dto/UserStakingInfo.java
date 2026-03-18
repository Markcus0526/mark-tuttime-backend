package com.tuttime.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * User staking information DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStakingInfo {
    private BigDecimal totalStaked;
    private Integer activeStakes;
    private BigDecimal totalRewardsEarned;
    private List<StakingPosition> stakingPositions;
}

