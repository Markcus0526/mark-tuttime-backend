package com.tuttime.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PasswordResetConfirmRequest {
    @NotBlank
    private String token;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;
}