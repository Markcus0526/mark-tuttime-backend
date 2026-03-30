package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "DTO for verifying phone number via verification code")
public class PhoneVerificationConfirmRequest {

	@Schema(description = "Phone number in E.164 format", example = "+1234567890")
	@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
	String phoneNumber;

	@Schema(description = "Verification code received via SMS")
	@NotBlank
	String code;
}
