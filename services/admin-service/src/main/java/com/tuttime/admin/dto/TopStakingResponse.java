package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Top Staking Response DTO for leaderboards
 *
 * <p>Represents a staking position in a leaderboard ranking by APY or mining bonus.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopStakingResponse {
	private Long stakingId;
	private Long userId;
	private String username;
	private Long poolId;
	private String poolName;
	private BigDecimal amount;
	private BigDecimal apyPercentage;
	private BigDecimal miningBonus;
	private BigDecimal rewardsEarned;
	private Integer rank;
	private String status; // ACTIVE, COMPLETED, etc.
}

