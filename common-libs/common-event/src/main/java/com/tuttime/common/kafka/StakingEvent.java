package com.tuttime.common.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Kafka event for staking-related events
 * Published by Staking Service, consumed by other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StakingEvent {

	public enum StakingEventType {
		STAKING_STARTED,
		STAKING_UNLOCKED,
		STAKING_REWARDS_EARNED,
		STAKING_COMPLETED
	}

	@JsonProperty("event_type")
	private StakingEventType eventType;

	@JsonProperty("user_id")
	private Long userId;

	@JsonProperty("staking_id")
	private Long stakingId;

	@JsonProperty("pool_id")
	private Long poolId;

	@JsonProperty("amount")
	private BigDecimal amount;

	@JsonProperty("rewards_amount")
	private BigDecimal rewardsAmount;

	@JsonProperty("mining_speed_boost")
	private BigDecimal miningSpeedBoost; // Percentage boost

	@JsonProperty("metadata")
	private Map<String, Object> metadata;

	@JsonProperty("timestamp")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime timestamp;

	@JsonProperty("version")
	@Builder.Default
	private String version = "1.0.0"; // Event schema version for evolution support
}

