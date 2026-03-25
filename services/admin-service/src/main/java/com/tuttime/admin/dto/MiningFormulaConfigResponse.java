package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Response DTO for mining formula configuration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiningFormulaConfigResponse {

	private Long configId;

	// Base Speed Parameters
	private BigDecimal baseSpeedUnverified;
	private BigDecimal baseSpeedVerified;

	// Friend Bonus Parameters
	private BigDecimal friendBonusPerFriend;
	private Integer maxFriends;
	private BigDecimal maxFriendBonus;

	// Referral Boost Parameters
	private BigDecimal referralBoostPerInvitation;

	// Booking Bonus Parameters
	private BigDecimal bookingBonusPerBooking;
	private Integer bookingBoostDurationDays;

	// Speed Cap
	private BigDecimal maxMiningSpeed;

	// Mining Interval Parameters
	private Integer minMiningIntervalMinutes;
	private Integer maxMiningIntervalMinutes;

	// Monthly Pool
	private BigDecimal monthlyPoolAmount;

	// Metadata
	private String description;
	private Boolean isActive;
	private Long updatedBy;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
}

