package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Staking Tier Request DTO
 * 
 * <p>Maps to staking_pools table structure in staking-service.
 * Used for both create and update operations.
 * For create operations, poolName, minStakeAmount, lockPeriodDays, apyPercentage, and boostMultiplier are required.
 * For update operations, all fields are optional.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // Ignore unknown fields like poolId, createdAt, updatedAt from response objects
public class StakingTierRequest {

    @Size(max = 100, message = "Pool name must not exceed 100 characters")
    private String poolName; // Maps to pool_name in staking_pools

    private String poolType; // FIXED_1_MONTH, FIXED_3_MONTHS, FIXED_6_MONTHS, FIXED_12_MONTHS, FLEXIBLE

    @DecimalMin(value = "0", inclusive = true, message = "Minimum stake amount must be non-negative")
    private BigDecimal minStakeAmount; // Maps to min_stake_amount

    @DecimalMin(value = "0", inclusive = true, message = "Maximum stake amount must be non-negative")
    private BigDecimal maxStakeAmount; // Maps to max_stake_amount (nullable)

    private Integer lockPeriodDays; // Maps to lock_period_days

    @DecimalMin(value = "0", inclusive = true, message = "APY percentage must be non-negative")
    private BigDecimal apyPercentage; // Maps to apy_percentage

    @DecimalMin(value = "0", inclusive = true, message = "Boost multiplier must be non-negative")
    private BigDecimal boostMultiplier; // Maps to boost_multiplier

    private Boolean isActive; // Maps to is_active

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description; // Maps to description

    // Legacy fields for backward compatibility (mapped to new fields)
    @Deprecated
    private String tierName; // Maps to poolName

    @Deprecated
    private BigDecimal apBoostPerHour; // Maps to boostMultiplier

    @Deprecated
    private Integer displayOrder; // Not used in staking_pools table
}
