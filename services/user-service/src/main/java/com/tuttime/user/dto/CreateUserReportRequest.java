package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a user report
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to report a user")
public class CreateUserReportRequest {
    
    @NotBlank(message = "Report category is required")
    @Schema(description = "Report category", example = "SPAM", 
            allowableValues = {"SPAM", "HARASSMENT", "FRAUD", "INAPPROPRIATE_CONTENT", "ILLEGAL_ACTIVITY", "OTHER"})
    private String reportCategory;
    
    @NotBlank(message = "Report reason is required")
    @Size(min = 10, max = 500, message = "Report reason must be between 10 and 500 characters")
    @Schema(description = "Reason for the report", example = "User is sending spam messages", minLength = 10, maxLength = 500)
    private String reportReason;
    
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    @Schema(description = "Additional description (optional)", example = "Additional details about the issue", maxLength = 2000)
    private String reportDescription;
}





