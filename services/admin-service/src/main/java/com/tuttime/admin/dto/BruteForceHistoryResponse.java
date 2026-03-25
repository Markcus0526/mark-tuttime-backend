package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Brute-Force History Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BruteForceHistoryResponse {

	private String historyId;
	private String ipAddress;
	private String phoneNumber;
	private Long attemptCount;
	private WarningSeverity severity;
	private String action; // BLOCKED_IP, BLOCKED_PHONE, PERMANENT_BLOCK, WARNING
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime detectedAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime blockedUntil; // null if not blocked or permanent
	private Boolean isPermanent;
	private String description;
}

