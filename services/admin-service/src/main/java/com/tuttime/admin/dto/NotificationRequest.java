package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Map;

/**
 * Request DTO for creating/updating notifications
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {
    
    @NotBlank(message = "Notification type is required")
    private String notificationType; // e.g., "SYSTEM_ALERT", "PROMOTIONAL_OFFER", "CUSTOM_NOTIFICATION"
    
    @NotNull(message = "Channel is required")
    private String channel; // EMAIL, SMS, PUSH, IN_APP
    
    private String subject;
    
    @NotBlank(message = "Message is required")
    private String message;
    
    @NotNull(message = "Priority is required")
    private String priority; // LOW, NORMAL, HIGH, URGENT
    
    // Targeting
    private Long userId; // null for broadcast to all users
    private List<Long> userIds; // For targeting specific users
    
    // Scheduling
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime scheduledAt; // null for immediate sending
    
    // References
    private String referenceType;
    private String referenceId;
    
    // Metadata (JSON)
    private Map<String, Object> metadata;
}

