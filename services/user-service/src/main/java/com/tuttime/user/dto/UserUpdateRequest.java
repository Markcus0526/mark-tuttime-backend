package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO for updating existing user information (partial updates allowed)")
public class UserUpdateRequest {

	@Schema(description = "User's phone number in international format", example = "+1234567890", nullable = true)
	@Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "Invalid phone number")
	private String phoneNumber;

	@Schema(description = "User's email address", example = "user@example.com", nullable = true)
	@Email(message = "Invalid email format")
	@Size(max = 255)
	private String email;

	@Schema(description = "User's full name", example = "John Doe", nullable = true)
	@Size(max = 255)
	private String fullName;

	@Schema(description = "URL to the user's profile picture", example = "https://example.com/pic.jpg", nullable = true)
	private String profilePicture;

}