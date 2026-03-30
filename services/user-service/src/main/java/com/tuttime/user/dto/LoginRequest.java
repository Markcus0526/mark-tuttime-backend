package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
	@Schema(description = "Email address or phone number for login", example = "user@example.com or +1234567890")
	@NotBlank(message = "Email or phone is required")
	String emailOrPhone;

	@Schema(description = "User password", example = "StrongPassword123!")
	@NotBlank(message = "Password is required")
	String password;
}