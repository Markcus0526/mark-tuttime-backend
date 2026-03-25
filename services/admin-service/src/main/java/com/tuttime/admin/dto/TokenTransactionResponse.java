package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Token Transaction Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenTransactionResponse {
	private String transactionId;
	private Long userId;
	private String type;
	private BigDecimal amount;
	private String status;
	private Long fromUserId;
	private Long toUserId;
	private String referenceType;
	private String referenceId;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
}

