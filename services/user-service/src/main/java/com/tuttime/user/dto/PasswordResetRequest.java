package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO to request a password reset token by email")
public class PasswordResetRequest {
	@Schema(description = "Email address", example = "user@example.com")
	@Email(message = "Invalid email")
	String email;
}
