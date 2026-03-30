package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "DTO representing user information in responses")
public class UserResponse {

	@Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
	private UUID id;

	@Schema(description = "User's phone number", example = "+1234567890", nullable = true)
	private String phoneNumber;

	@Schema(description = "User's email address", example = "user@example.com", nullable = true)
	private String email;

	@Schema(description = "User's full name", example = "John Doe")
	private String fullName;

	@Schema(description = "URL to user's profile picture", example = "https://example.com/pic.jpg", nullable = true)
	private String profilePicture;

	@Schema(description = "User's status", example = "ACTIVE")
	private String status;

	@Schema(description = "Timestamp of last login", example = "2024-01-01T12:00:00Z", nullable = true)
	private OffsetDateTime lastLogin;

	@Schema(description = "User creation timestamp", example = "2023-12-31T11:00:00Z")
	private OffsetDateTime createdAt;

	@Schema(description = "User last update timestamp", example = "2024-01-05T14:30:00Z")
	private OffsetDateTime updatedAt;

}