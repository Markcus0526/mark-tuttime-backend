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
 * Kafka event for token-related events
 * Published by Token Service, consumed by other services
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenEvent {

	public enum TokenEventType {
		BALANCE_UPDATED,
		DEPOSIT_CONFIRMED,
		WITHDRAWAL_REQUESTED,
		WITHDRAWAL_COMPLETED,
		WITHDRAWAL_FAILED,
		TOKEN_BURNED,
		TRANSACTION_CREATED
	}

	@JsonProperty("event_type")
	private TokenEventType eventType;

	@JsonProperty("user_id")
	private Long userId;

	@JsonProperty("transaction_id")
	private Long transactionId;

	@JsonProperty("amount")
	private BigDecimal amount;

	@JsonProperty("balance_after")
	private BigDecimal balanceAfter;

	@JsonProperty("transaction_type")
	private String transactionType; // DEPOSIT, WITHDRAWAL, TRANSFER, etc.

	@JsonProperty("status")
	private String status; // PENDING, CONFIRMED, FAILED, etc.

	@JsonProperty("reference_type")
	private String referenceType; // CONVERSION, REWARD, STAKING, etc.

	@JsonProperty("reference_id")
	private String referenceId;

	@JsonProperty("metadata")
	private Map<String, Object> metadata;

	@JsonProperty("timestamp")
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime timestamp;

	@JsonProperty("version")
	@Builder.Default
	private String version = "1.0.0"; // Event schema version for evolution support
}

