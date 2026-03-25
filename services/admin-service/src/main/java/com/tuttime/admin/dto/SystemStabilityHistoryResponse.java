package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * System Stability History Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStabilityHistoryResponse {

	private String historyId;
	private WarningSeverity severity;
	private String eventType; // ERROR_RATE, RESPONSE_TIME, MEMORY_USAGE, CPU_USAGE, DATABASE_ERROR, etc.
	private String title;
	private String description;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime occurredAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime resolvedAt;
	private Boolean resolved;
	private String serviceName; // e.g., "user-service", "token-service"
	private Map<String, Object> metrics; // Additional metrics data
}

