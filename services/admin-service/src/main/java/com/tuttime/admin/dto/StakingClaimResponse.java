package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Staking Claim Response DTO
 *
 * <p>Represents a reward claim operation in the staking history.
 * Claims are typically tracked through token transactions with type "CLAIM".
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakingClaimResponse {
	/**
	 * Transaction ID (from token_transactions)
	 */
	private Long transactionId;

	/**
	 * User ID
	 */
	private Long userId;

	/**
	 * Username (first name + last name)
	 */
	private String username;

	/**
	 * Staking ID (reference)
	 */
	private Long stakingId;

	/**
	 * Pool ID
	 */
	private Long poolId;

	/**
	 * Pool name
	 */
	private String poolName;

	/**
	 * Claimed reward amount
	 */
	private BigDecimal claimedAmount;

	/**
	 * Status (CONFIRMED, PENDING, FAILED)
	 */
	private String status;

	/**
	 * Claimed at timestamp
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime claimedAt;

	/**
	 * Created timestamp
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
}

