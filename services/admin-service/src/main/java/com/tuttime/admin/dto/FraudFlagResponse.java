package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Fraud Flag Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudFlagResponse {
	private Long flagId;
	private Long userId;
	private String flagType; // MULTI_DEVICE, MULTI_ACCOUNT_IP, SUSPICIOUS_ACTIVITY
	private WarningSeverity severity; // LOW, MEDIUM, HIGH, CRITICAL
	private WarningCategory category; // MALICIOUS, SUSPICIOUS, FRAUD, MULTI_ACCOUNT, MULTI_DEVICE, etc.
	private String description;
	private String deviceId;
	private String ipAddress;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	private Boolean resolved;
}

