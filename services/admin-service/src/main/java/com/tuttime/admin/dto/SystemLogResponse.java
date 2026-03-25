package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * System Log Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemLogResponse {
	private Long logId;
	private String serviceName;
	private String logLevel;
	private String loggerName;
	private String message;
	private String stackTrace;
	private String threadName;
	private String hostName;
	private String ipAddress;
	private Long userId;
	private String requestId;
	private String traceId;
	private String spanId;
	private Map<String, Object> metadata;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
}

