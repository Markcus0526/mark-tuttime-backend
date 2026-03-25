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
 * Mining Formula Configuration Entity
 *
 * @deprecated This entity is deprecated. The mining_formula_config table has been merged into system_config.
 * All mining formula configuration fields are now in {@link SystemConfig} entity.
 *
 * <p>This class is kept for backward compatibility but should not be used in new code.
 * Use {@link SystemConfig} and {@link com.tuttime.admin.service.AdminMiningFormulaService} instead.
 *
 * <p>Stores configurable mining formula parameters.
 * Uses singleton pattern - only one row should exist (config_id = 1).
 */
@Deprecated
@Entity
@Table(name = "mining_formula_config", schema = "admin_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MiningFormulaConfig {

	@Id
	@Column(name = "config_id")
	@Builder.Default
	private Long configId = 1L; // Singleton - always use ID 1

	// Base Speed Parameters
	@Column(name = "base_speed_unverified", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal baseSpeedUnverified = new BigDecimal("0.3");

	@Column(name = "base_speed_verified", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal baseSpeedVerified = new BigDecimal("1.0");

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
	@Column(name = "booking_bonus_per_booking", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal bookingBonusPerBooking = new BigDecimal("2.0");

	@Column(name = "booking_boost_duration_days", nullable = false)
	@Builder.Default
	private Integer bookingBoostDurationDays = 15;

	// Speed Cap
	@Column(name = "max_mining_speed", nullable = false, precision = 10, scale = 4)
	@Builder.Default
	private BigDecimal maxMiningSpeed = new BigDecimal("77.777");

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

	@Column(name = "updated_by")
	private Long updatedBy;

	@Column(name = "created_at", nullable = false, updatable = false)
	@CreatedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;

	@Column(name = "updated_at", nullable = false)
	@LastModifiedDate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
}

