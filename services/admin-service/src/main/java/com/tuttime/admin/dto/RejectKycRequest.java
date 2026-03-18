package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Reject KYC Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RejectKycRequest {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}

