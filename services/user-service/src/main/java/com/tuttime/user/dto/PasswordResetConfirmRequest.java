package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO to confirm and complete password reset")
public class PasswordResetConfirmRequest {

	@Schema(description = "Password reset token")
	@NotBlank(message = "Reset token is required")
	private String token;

	@Schema(description = "New password for the user", minLength = 8)
	@NotBlank(message = "New password is required")
	@Size(min = 8, message = "Password must be at least 8 characters")
	private String newPassword;

}