package com.tuttime.admin.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for updating mining formula configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningFormulaConfigRequest {

	// Base Speed Parameters
	@DecimalMin(value = "0.0", message = "Base speed unverified must be >= 0")
	private BigDecimal baseSpeedUnverified;

	@DecimalMin(value = "0.0", message = "Base speed verified must be >= 0")
	private BigDecimal baseSpeedVerified;

	// Friend Bonus Parameters
	@DecimalMin(value = "0.0", message = "Friend bonus per friend must be >= 0")
	private BigDecimal friendBonusPerFriend;

	@Min(value = 1, message = "Max friends must be > 0")
	private Integer maxFriends;

	@DecimalMin(value = "0.0", message = "Max friend bonus must be >= 0")
	private BigDecimal maxFriendBonus;

	// Referral Boost Parameters
	@DecimalMin(value = "0.0", message = "Referral boost per invitation must be >= 0")
	private BigDecimal referralBoostPerInvitation;

	// Booking Bonus Parameters
	@DecimalMin(value = "0.0", message = "Booking bonus per booking must be >= 0")
	private BigDecimal bookingBonusPerBooking;

	@Min(value = 1, message = "Booking boost duration days must be > 0")
	private Integer bookingBoostDurationDays;

	// Speed Cap
	@DecimalMin(value = "0.1", message = "Max mining speed must be > 0")
	private BigDecimal maxMiningSpeed;

	// Mining Interval Parameters
	@Min(value = 1, message = "Min mining interval minutes must be > 0")
	private Integer minMiningIntervalMinutes;

	@Min(value = 1, message = "Max mining interval minutes must be > 0")
	private Integer maxMiningIntervalMinutes;

	// Monthly Pool
	@DecimalMin(value = "0.0", message = "Monthly pool amount must be >= 0")
	private BigDecimal monthlyPoolAmount;

	// Metadata
	private String description;

	private Boolean isActive;
}

