package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * AP Balance Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApBalanceResponse {
	private Long userId;
	private String username;
	private String email;
	private BigDecimal currentBalance; // Changed from Long to BigDecimal to preserve decimal precision
	private BigDecimal totalEarned; // Changed from Long to BigDecimal to preserve decimal precision
	private BigDecimal totalSpent; // Changed from Long to BigDecimal to preserve decimal precision
	private String statusLevel; // BRONZE, SILVER, GOLD, PLATINUM, ELITE
	private Integer referralCount;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime lastUpdated;
}

