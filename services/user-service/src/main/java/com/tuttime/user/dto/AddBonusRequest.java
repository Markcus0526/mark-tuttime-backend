package com.tuttime.user.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * Request DTO for adding bonus to user account
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddBonusRequest {
    @NotNull(message = "bonusType is required")
    @Pattern(regexp = "AP|APART", message = "bonusType must be either AP or APART")
    private String bonusType;
    
    @NotNull(message = "amount is required")
    private BigDecimal amount;
    
    private String reason;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC")
    private OffsetDateTime expiresAt;
}

