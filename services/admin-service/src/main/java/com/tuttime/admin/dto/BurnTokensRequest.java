package com.tuttime.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Burn Tokens Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BurnTokensRequest {
    @NotNull(message = "Amount is required")
    private BigDecimal amount;
    
    @NotBlank(message = "Reason is required")
    private String reason;
}

