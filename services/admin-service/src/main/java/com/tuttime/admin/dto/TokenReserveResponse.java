package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Token Reserve Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenReserveResponse {
	private Long reserveId;
	private String network;
	private BigDecimal treasuryBalance;
	private BigDecimal hotWalletBalance;
	private BigDecimal totalBalance;
	private BigDecimal tokenPrice;
	private BigDecimal totalSupply;
	private BigDecimal nav;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime lastUpdatedAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
}

