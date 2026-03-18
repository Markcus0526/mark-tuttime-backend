package com.tuttime.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for email verification during password reset
 * 
 * @deprecated Use {@link VerifyCodeForPasswordResetRequest} instead.
 * This DTO is kept for backward compatibility but is no longer used in the password reset flow.
 */
@Deprecated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailForPasswordResetRequest {
    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Email verification code is required")
    @Size(min = 4, max = 4, message = "Verification code must be 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "Verification code must be 4 digits")
    private String emailCode;
}

