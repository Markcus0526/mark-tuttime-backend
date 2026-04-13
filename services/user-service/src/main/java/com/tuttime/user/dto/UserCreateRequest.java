package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO for creating a new user")
public class UserCreateRequest {

	@Schema(description = "Email address or phone number for login", example = "user@example.com or +1234567890")
	@NotBlank(message = "Email or phone is required")
	@Size(max = 255)
	String emailOrPhone;

	@Schema(description = "User's role name", example = "OWNER", nullable = true)
	private String roleName;
}

