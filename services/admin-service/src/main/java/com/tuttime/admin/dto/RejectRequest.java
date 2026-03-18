package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for rejecting ads or videos
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectRequest {
    
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}

