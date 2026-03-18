package com.tuttime.user.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Response DTO for user report information
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User report information")
public class UserReportResponse {
    
    @Schema(description = "Report ID", example = "1")
    private Long reportId;
    
    @Schema(description = "Reporter user ID", example = "123")
    private Long reporterUserId;
    
    @Schema(description = "Reporter username", example = "reporter_user")
    private String reporterUsername;
    
    @Schema(description = "Reported user ID", example = "456")
    private Long reportedUserId;
    
    @Schema(description = "Reported username", example = "reported_user")
    private String reportedUsername;
    
    @Schema(description = "Report category", example = "SPAM")
    private String reportCategory;
    
    @Schema(description = "Report reason", example = "User is sending spam messages")
    private String reportReason;
    
    @Schema(description = "Report description", example = "Additional details")
    private String reportDescription;
    
    @Schema(description = "When the report was created")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime createdAt;
}





