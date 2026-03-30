package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "DTO to request a password reset token by email or phone")
public class PasswordResetRequest {
	@Schema(description = "Email or phone number of the user", example = "user@example.com or +1234567890")
	@NotBlank
	String emailOrPhone;
}
