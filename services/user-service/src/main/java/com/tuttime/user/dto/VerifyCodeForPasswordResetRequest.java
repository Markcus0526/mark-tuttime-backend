package com.tuttime.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for email or SMS verification during password reset
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyCodeForPasswordResetRequest {
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Verification code is required")
    @Size(min = 4, max = 4, message = "Verification code must be 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "Verification code must be 4 digits")
    private String code;
}

