package com.tuttime.admin.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * System Configuration Entity
 *
 * <p>Stores system-wide configuration parameters including mining formula configuration.
 * Uses singleton pattern - only one row should exist (id = 1).
 *
 * <p>This entity now includes all fields previously in mining_formula_config table,
 * which has been merged into system_config for unified configuration management.
 */
@Entity
@Table(name = "system_config", schema = "admin_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class SystemConfig {

	@Id
	@Column(name = "config_id")
	@Builder.Default
	private Long configId = 1L; // Singleton - always use ID 1

	// Mining Parameters (merged from mining_formula_config)
	// Base Speed Parameters
	@Column(name = "base_speed_unverified", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal baseSpeedUnverified = new BigDecimal("0.3");

	@Column(name = "base_speed_verified", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal baseSpeedVerified = new BigDecimal("1.0");

	// Legacy mining speed field (deprecated - use base_speed_verified instead)
	@Column(name = "base_mining_speed", precision = 10, scale = 3)
	private BigDecimal baseMiningSpeed;

	// Friend Bonus Parameters
	@Column(name = "friend_bonus_per_friend", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal friendBonusPerFriend = new BigDecimal("0.5");

	@Column(name = "max_friends", nullable = false)
	@Builder.Default
	private Integer maxFriends = 10;

	@Column(name = "max_friend_bonus", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal maxFriendBonus = new BigDecimal("5.0");

	// Referral Boost Parameters
	@Column(name = "referral_boost_per_invitation", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal referralBoostPerInvitation = new BigDecimal("0.5");

	// Booking Bonus Parameters
	@Column(name = "booking_bonus_per_booking", precision = 10, scale = 4)
	private BigDecimal bookingBonusPerBooking;

	@Column(name = "booking_boost_duration_days")
	private Integer bookingBoostDurationDays;

	// Legacy booking boost fields (deprecated)
	@Column(name = "booking_boost_base", precision = 10, scale = 3)
	private BigDecimal bookingBoostBase;

	// Speed Cap
	@Column(name = "max_mining_speed", precision = 10, scale = 4)
	private BigDecimal maxMiningSpeed;

	// Mining Interval Parameters
	@Column(name = "min_mining_interval_minutes", nullable = false)
	@Builder.Default
	private Integer minMiningIntervalMinutes = 1;

	@Column(name = "max_mining_interval_minutes", nullable = false)
	@Builder.Default
	private Integer maxMiningIntervalMinutes = 5;

	// Monthly Pool
	@Column(name = "monthly_pool_amount", nullable = false, precision = 36, scale = 18)
	@Builder.Default
	private BigDecimal monthlyPoolAmount = new BigDecimal("50000000000.0"); // 50B APARTs

	// Metadata
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "is_active", nullable = false)
	@Builder.Default
	private Boolean isActive = true;

	// Rewards Boost Parameters (other)
	@Column(name = "max_referral_boosts")
	private Integer maxReferralBoosts;

	@Column(name = "rental_boost_multiplier", precision = 10, scale = 3)
	private BigDecimal rentalBoostMultiplier;

	@Column(name = "rental_boost_duration_days")
	private Integer rentalBoostDurationDays;

	// Extra Parameters
	@Column(name = "withdrawal_platform_fee_percent", precision = 5, scale = 2)
	private BigDecimal withdrawalPlatformFeePercent;

	@Column(name = "deposit_minimum_amount", precision = 36, scale = 18)
	private BigDecimal depositMinimumAmount;

	@Column(name = "withdrawal_minimum_amount", precision = 36, scale = 18)
	private BigDecimal withdrawalMinimumAmount;

	@Column(name = "withdrawal_maximum_amount", precision = 36, scale = 18)
	private BigDecimal withdrawalMaximumAmount;

	@Column(name = "withdrawal_daily_limit")
	private Integer withdrawalDailyLimit;

	// Legacy fields for backward compatibility
	@Column(name = "monthly_pool", precision = 36, scale = 18)
	private BigDecimal monthlyPool;

	@Column(name = "max_referral_bonus")
	private Integer maxReferralBonus;

	@Column(name = "referral_bonus_per_active", precision = 10, scale = 3)
	private BigDecimal referralBonusPerActive;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	@LastModifiedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;

	@Column(name = "updated_by")
	private Long updatedBy;
}

