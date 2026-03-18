package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.OffsetDateTime;

/**
 * Request DTO for scheduling videos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleVideoRequest {
    
    @NotNull(message = "Scheduled date is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime scheduledAt;
}

