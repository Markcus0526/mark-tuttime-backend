package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Update System Config Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemConfigRequest {
	// Mining Parameters
	private BigDecimal baseMiningSpeed; // 0.3 AP/hr
	private BigDecimal maxMiningSpeed; // 77.777 AP/hr
	private Integer minMiningIntervalMinutes; // 1 minute
	private Integer maxMiningIntervalMinutes; // 5 minutes
	private BigDecimal monthlyPoolAmount; // 50,000,000,000 APART (50B)

	// Rewards Boost Parameters
	// Note: referralBoostPerInvitation is now in system_config table (merged from mining_formula_config)
	// Use AdminMiningFormulaService API to update mining formula parameters
	private Integer maxReferralBoosts; // 10 referrals per user
	private BigDecimal bookingBoostBase; // +2 AP/hr
	private Integer bookingBoostDurationDays; // 15 days
	private BigDecimal rentalBoostMultiplier; // * 2 AP/hr
	private Integer rentalBoostDurationDays; // 2 days

	// Extra Parameters (Platform Fees, Limits, etc.)
	private BigDecimal withdrawalPlatformFeePercent; // 0% currently
	private BigDecimal depositMinimumAmount; // if any
	private BigDecimal withdrawalMinimumAmount; // if any
	private BigDecimal withdrawalMaximumAmount; // if any
	private Integer withdrawalDailyLimit; // if any

	// Legacy fields (for backward compatibility)
	@Deprecated
	private BigDecimal monthlyPool; // Deprecated: use monthlyPoolAmount instead
	@Deprecated
	private Integer maxReferralBonus; // +2 AP/hour max
	@Deprecated
	private BigDecimal referralBonusPerActive; // +0.2 AP/hour per active referral
}

