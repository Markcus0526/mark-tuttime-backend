package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.Map;

/**
 * Request DTO for updating a notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationRequest {
    
    private String title;
    private String message;
    private String targetAudience;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime scheduledAt;
    private String notificationType;
    private String channel;
    private String priority;
    private List<Long> userIds;
    private String referenceType;
    private String referenceId;
    private Map<String, Object> metadata;
}

