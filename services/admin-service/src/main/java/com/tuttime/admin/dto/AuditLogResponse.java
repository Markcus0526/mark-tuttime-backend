package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tuttime.admin.entity.AdminAuditLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Audit Log Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {

	private Long logId;
	private Long adminId;
	private String adminUsername;
	private AdminAuditLog.AdminActionType actionType;
	private String resourceType;
	private Long resourceId;
	private String description;
	private String ipAddress;
	private String userAgent;
	private String requestMethod;
	private String requestPath;
	private String requestBody;
	private Integer responseStatus;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
}

