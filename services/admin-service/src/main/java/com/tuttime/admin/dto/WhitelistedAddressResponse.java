package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for whitelisted token address information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhitelistedAddressResponse {

	private String address; // Wallet/blockchain address
	private String reason; // Optional reason for whitelisting
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime whitelistedAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime expiresAt; // null for permanent whitelist
	private Boolean isPermanent;
	private String whitelistedBy; // Admin who whitelisted the address
}

