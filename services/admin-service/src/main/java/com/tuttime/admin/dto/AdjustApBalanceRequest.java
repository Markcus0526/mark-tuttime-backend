package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Adjust AP Balance Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustApBalanceRequest {
    @NotNull(message = "Amount is required")
    private Long amount; // Positive to add, negative to subtract
    
    @NotBlank(message = "Reason is required")
    private String reason;
}

