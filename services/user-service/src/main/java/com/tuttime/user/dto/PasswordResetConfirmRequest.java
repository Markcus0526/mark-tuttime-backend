package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO to confirm and complete password reset")
public class PasswordResetConfirmRequest {

	@Schema(description = "Email address", example = "user@example.com")
	@Email(message = "Invalid email")
	String email;

	@Schema(description = "Password reset OTP code")
	@NotBlank(message = "Reset OTP code is required")
	private String code;

	@Schema(description = "New password for the user", minLength = 8)
	@NotBlank(message = "New password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String newPassword;

}