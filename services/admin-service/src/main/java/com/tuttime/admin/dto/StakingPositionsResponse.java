package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Staking Positions Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakingPositionsResponse {
	private Long userId;
	private List<StakingPosition> positions;

	@Data
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class StakingPosition {
		private Long stakingId;
		private Long poolId;
		private BigDecimal amount;
		private BigDecimal boostMultiplier;
		private BigDecimal rewardsEarned;
		private String status;
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
		private OffsetDateTime createdAt;
		@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
		private OffsetDateTime unlockAt;
	}
}

