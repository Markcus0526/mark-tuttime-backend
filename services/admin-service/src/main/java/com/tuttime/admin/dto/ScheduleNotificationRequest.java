package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Request DTO for scheduling a notification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleNotificationRequest {
    
    @NotNull(message = "Scheduled time is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime scheduledAt;
}

