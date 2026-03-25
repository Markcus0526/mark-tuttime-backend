package com.tuttime.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Response DTO for notification details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

	private Long notificationId;
	private Long userId; // null for broadcast notifications
	private String notificationType;
	private String channel; // EMAIL, SMS, PUSH, IN_APP
	private String subject;
	private String message;
	private String status; // PENDING, SENT, DELIVERED, FAILED, CANCELLED
	private String priority; // LOW, NORMAL, HIGH, URGENT
	private String referenceType;
	private String referenceId;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime scheduledAt; // null if immediate
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime sentAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime createdAt;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime updatedAt;
	private Boolean isRead;
	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
	private OffsetDateTime readAt;
	private Integer retryCount;
	private String errorMessage;
	private Map<String, Object> metadata;

	// Broadcast info
	private Boolean isBroadcast; // true if sent to all users
	private Long totalRecipients; // Number of users who received this (for broadcasts)
}

