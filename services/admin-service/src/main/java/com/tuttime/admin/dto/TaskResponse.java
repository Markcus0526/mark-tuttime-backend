package com.tuttime.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Response DTO for task details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskResponse {
    
    private Long taskId;
    private String taskTitle;
    private String taskDescription;
    private String taskType;
    private BigDecimal bonusAp; // Mining speed bonus in AP/hour when task is completed
    private String status; // INACTIVE, ACTIVE
    private Integer maxCompletions;
    private Integer currentCompletions;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime startDate;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime endDate;
    private Boolean isRepeatable;
    private Integer repeatIntervalDays;
    private String actionType; // WATCH_VIDEO, APART_SHARING_TASK, DAILY_TASK
    private Map<String, Object> metadata;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime updatedAt;
    private Long createdBy;
    private String thumbnailUrl; // Thumbnail URL for video tasks
}

