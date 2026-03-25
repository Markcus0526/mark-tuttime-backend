package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Staking Tier Response DTO
 *
 * <p>Maps to staking_pools table structure in staking-service.
 * Includes all fields from the database table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakingTierResponse {

	// Primary fields matching staking_pools table
	private Long poolId; // Maps to pool_id
	private String poolName; // Maps to pool_name
	private String poolType; // Maps to pool_type (FIXED_1_MONTH, FIXED_3_MONTHS, FIXED_6_MONTHS, FIXED_12_MONTHS, FLEXIBLE)
	private BigDecimal minStakeAmount; // Maps to min_stake_amount
	private BigDecimal maxStakeAmount; // Maps to max_stake_amount (nullable)
	private Integer lockPeriodDays; // Maps to lock_period_days
	private BigDecimal apyPercentage; // Maps to apy_percentage
	private BigDecimal boostMultiplier; // Maps to boost_multiplier
	private Boolean isActive; // Maps to is_active
	private String description; // Maps to description
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt; // Maps to created_at
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt; // Maps to updated_at

	// Legacy fields for backward compatibility
	@Deprecated
	private Long tierId; // Maps to poolId

	@Deprecated
	private String tierName; // Maps to poolName

	@Deprecated
	private BigDecimal apBoostPerHour; // Maps to boostMultiplier

	@Deprecated
	private Integer displayOrder; // Not in staking_pools table, kept for compatibility

	@Deprecated
	private Long createdBy; // Not in staking_pools table, kept for compatibility
}

