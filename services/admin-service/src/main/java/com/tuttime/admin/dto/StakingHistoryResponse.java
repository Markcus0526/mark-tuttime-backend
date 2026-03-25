package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Staking History Response DTO
 *
 * <p>Represents a stake or unstake operation in the staking history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakingHistoryResponse {
	/**
	 * Staking ID
	 */
	private Long stakingId;

	/**
	 * User ID
	 */
	private Long userId;

	/**
	 * Username (first name + last name)
	 */
	private String username;

	/**
	 * Pool ID
	 */
	private Long poolId;

	/**
	 * Pool name
	 */
	private String poolName;

	/**
	 * Staked amount
	 */
	private BigDecimal amount;

	/**
	 * Status (ACTIVE, UNLOCKING, COMPLETED, CANCELLED)
	 */
	private String status;

	/**
	 * Staked at timestamp
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime stakedAt;

	/**
	 * Unstake requested at timestamp (for unstakes)
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime unstakeRequestedAt;

	/**
	 * Unlock at timestamp
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime unlockAt;

	/**
	 * Operation type (STAKE or UNSTAKE)
	 */
	private String operationType;
}

