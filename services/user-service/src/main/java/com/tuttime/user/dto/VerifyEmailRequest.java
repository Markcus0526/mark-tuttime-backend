package com.tuttime.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for email verification
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyEmailRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Verification code is required")
    @Size(min = 4, max = 4, message = "Verification code must be 4 digits")
    @Pattern(regexp = "^\\d{4}$", message = "Verification code must be 4 digits")
    private String code;
}

