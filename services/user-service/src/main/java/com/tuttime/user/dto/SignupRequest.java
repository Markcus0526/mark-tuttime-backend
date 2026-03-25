package com.tuttime.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SignupRequest {
	@Pattern(regexp = "^\\+?[0-9]{7,15}$", message = "Invalid phone number")
	private String phoneNumber;

	@Email(message = "Invalid email")
	private String email;

	@NotBlank
	private String fullName;

	@Size(min = 6, message = "Password must be at least 6 characters")
	private String password;
}
