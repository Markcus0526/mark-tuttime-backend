package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {
	@Schema(description = "Phone number in E.164 format", example = "+1234567890", nullable = true)
	@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
	String phoneNumber;

	@Schema(description = "Email address", example = "user@example.com", nullable = true)
	@Email(message = "Invalid email")
	String email;

	@Schema(description = "User password", example = "StrongPassword123!")
	@NotBlank(message = "Password is required")
	String password;

//	@Schema(description = "Full name of the user", example = "John Doe", nullable = true)
//	@NotBlank(message = "Full name is required")
//	String fullName;
}
