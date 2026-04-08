package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpVerificationRequest {
	@Schema(description = "Phone number in E.164 format", example = "+1234567890")
	@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
	String phoneNumber;

	@Schema(description = "Email address", example = "user@example.com")
	@Email(message = "Invalid email")
	String email;
}

