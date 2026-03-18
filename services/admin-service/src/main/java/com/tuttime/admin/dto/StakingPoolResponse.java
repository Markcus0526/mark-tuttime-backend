package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Staking Pool Response DTO
 * 
 * <p>Maps to staking_pools table structure in staking-service.
 * Includes all fields from the database table.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS) // Always include fields, even if null
public class StakingPoolResponse {
    // Primary fields matching staking_pools table
    @JsonProperty("poolId")
    private Long poolId; // Maps to pool_id
    
    @JsonProperty("poolName")
    private String poolName; // Maps to pool_name
    
    @JsonProperty("poolType")
    private String poolType; // Maps to pool_type (FIXED_1_MONTH, FIXED_3_MONTHS, FIXED_6_MONTHS, FIXED_12_MONTHS, FLEXIBLE)
    
    @JsonProperty("minStakeAmount")
    private BigDecimal minStakeAmount; // Maps to min_stake_amount
    
    @JsonProperty("maxStakeAmount")
    private BigDecimal maxStakeAmount; // Maps to max_stake_amount (nullable)
    
    @JsonProperty("lockPeriodDays")
    private Integer lockPeriodDays; // Maps to lock_period_days
    
    @JsonProperty("apyPercentage")
    private BigDecimal apyPercentage; // Maps to apy_percentage
    
    @JsonProperty("boostMultiplier")
    private BigDecimal boostMultiplier; // Maps to boost_multiplier
    
    @JsonProperty("isActive")
    private Boolean isActive; // Maps to is_active
    
    @JsonProperty("description")
    private String description; // Maps to description
    
    @JsonProperty("createdAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt; // Maps to created_at
    
    @JsonProperty("updatedAt")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt; // Maps to updated_at
    
    // Legacy fields for backward compatibility (hidden from JSON)
    @Deprecated
    @JsonIgnore
    private String name; // Maps to poolName
    
    @Deprecated
    @JsonIgnore
    private BigDecimal minAmount; // Maps to minStakeAmount
    
    @Deprecated
    @JsonIgnore
    private BigDecimal maxAmount; // Maps to maxStakeAmount
    
    @Deprecated
    @JsonIgnore
    private BigDecimal totalStaked; // Not in staking_pools table, kept for compatibility
    
    @Deprecated
    @JsonIgnore
    private Integer activePositions; // Not in staking_pools table, kept for compatibility
}

