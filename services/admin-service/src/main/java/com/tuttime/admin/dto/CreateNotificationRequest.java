package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * Request DTO for creating a notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateNotificationRequest {

	private String title; // Deprecated: use titleEn instead (optional for backward compatibility)

	private String message; // Deprecated: use messageEn instead (optional for backward compatibility)

	@NotBlank(message = "English title is required")
	private String titleEn;

	@NotBlank(message = "English message is required")
	private String messageEn;

	@NotBlank(message = "Russian title is required")
	private String titleRu;

	@NotBlank(message = "Russian message is required")
	private String messageRu;

	@NotBlank(message = "Target audience is required")
	private String targetAudience; // "ALL" or "SPECIFIC_USERS"

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime scheduledAt; // null for immediate sending

	// Optional fields
	private String notificationType;
	private String channel; // EMAIL, SMS, PUSH, IN_APP
	private String priority; // LOW, NORMAL, HIGH, URGENT
	private List<Long> userIds; // For SPECIFIC_USERS
	private String referenceType;
	private String referenceId;
	private Map<String, Object> metadata;
}

