package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Global Transaction Response DTO
 *
 * <p>Represents a transaction (deposit/withdrawal/burn) in the global history.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalTransactionResponse {
	/**
	 * Order/Request ID (deposit_id, withdrawal_id, or burn_id)
	 */
	private Long orderRequestId;

	/**
	 * User ID
	 */
	private Long userId;

	/**
	 * Username (first name + last name)
	 */
	private String username;

	/**
	 * Transaction amount
	 */
	private java.math.BigDecimal amount;

	/**
	 * Wallet address (for deposits/withdrawals)
	 */
	private String walletAddress;

	/**
	 * Transaction hash (blockchain tx hash)
	 */
	private String txHash;

	/**
	 * Processed state/status
	 */
	private String processedState;

	/**
	 * Network (e.g., "Ethereum", "BSC", "Polygon")
	 */
	private String network;

	/**
	 * Transaction type (DEPOSIT, WITHDRAWAL, BURN)
	 */
	private String transactionType;

	/**
	 * Created timestamp
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;

	/**
	 * Updated timestamp
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;

	/**
	 * Processed/Completed timestamp
	 */
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime processedAt;
}

