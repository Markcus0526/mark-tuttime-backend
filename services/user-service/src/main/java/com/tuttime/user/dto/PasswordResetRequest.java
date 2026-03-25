package com.tuttime.user.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PasswordResetRequest {
    @Email
    @NotBlank
    private String email;
}
