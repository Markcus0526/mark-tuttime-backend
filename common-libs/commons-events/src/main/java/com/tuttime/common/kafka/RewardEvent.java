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
 * Kafka event for reward-related events
 * Published by Rewards Service, consumed by other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RewardEvent {

	public enum RewardEventType {
		REFERRAL_CREATED,
		REFERRAL_REWARD_GRANTED,
		ACTIVE_FRIEND_ADDED,
		ACTIVE_FRIEND_REWARD_GRANTED,
		REWARD_GRANTED,
		REWARD_PAID
	}

	@JsonProperty("event_type")
	private RewardEventType eventType;

	@JsonProperty("user_id")
	private Long userId;

	@JsonProperty("referrer_id")
	private Long referrerId;

	@JsonProperty("referee_id")
	private Long refereeId;

	@JsonProperty("referral_id")
	private Long referralId;

	@JsonProperty("reward_type")
	private String rewardType; // ACTIVE_FRIEND, REFERRAL_BONUS, etc.

	@JsonProperty("amount")
	private BigDecimal amount;

	@JsonProperty("reference_type")
	private String referenceType;

	@JsonProperty("reference_id")
	private Long referenceId;

	@JsonProperty("metadata")
	private Map<String, Object> metadata;

	@JsonProperty("timestamp")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime timestamp;

	@JsonProperty("version")
	@Builder.Default
	private String version = "1.0.0"; // Event schema version for evolution support
}

