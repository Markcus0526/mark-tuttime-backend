package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Warning Response DTO
 *
 * <p>Represents a consolidated warning from various sources:
 * <ul>
 *   <li>Fraud flags</li>
 *   <li>Brute-force attempts</li>
 *   <li>System stability issues</li>
 *   <li>Other security warnings</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningResponse {

	private String warningId;
	private WarningCategory category;
	private WarningSeverity severity;
	private String message; // Main warning message (can be derived from title/description)
	private String title;
	private String description;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime resolvedAt;
	private Boolean resolved;

	// Context information
	private Long userId;
	private String deviceId;
	private String ipAddress;
	private String phoneNumber;
	private String walletAddress;

	// Additional metadata
	private Map<String, Object> metadata;

	// Source information
	private String source; // e.g., "ANTI_FRAUD", "BRUTE_FORCE", "SYSTEM_STABILITY"
}

