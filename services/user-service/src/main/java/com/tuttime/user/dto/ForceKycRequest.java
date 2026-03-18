package com.tuttime.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for forcing KYC verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForceKycRequest {
    @NotNull(message = "completed field is required")
    private Boolean completed;
    private String reason;
}

