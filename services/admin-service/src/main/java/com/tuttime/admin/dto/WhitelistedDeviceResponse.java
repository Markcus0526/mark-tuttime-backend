package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for whitelisted device information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WhitelistedDeviceResponse {

	private String deviceId;
	private String reason; // Optional reason for whitelisting
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime whitelistedAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime expiresAt; // null for permanent whitelist
	private Boolean isPermanent;
}

