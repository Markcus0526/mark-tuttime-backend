package com.tuttime.user.dto;

import com.tuttime.user.entity.enums.AuthProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserDto {

	@Schema(description = "DTO for creating a new user")
	public record UserCreateRequest(

			@Schema(description = "Email address or phone number for login", example = "user@example.com or +1234567890")
			@NotBlank(message = "Email or phone is required")
			@Size(max = 255)
			String emailOrPhone,

			@Schema(description = "User's role name", example = "USER", nullable = true)
			String roleName,

			@Schema(description = "User's UI language", example = "en or ru", nullable = true)
			String language
	) {
	}

	@Schema(description = "DTO for updating existing user information (partial updates allowed)")
	public record UserUpdateRequest(

			@Schema(description = "User's phone number in international format", example = "+1234567890", nullable = true)
			@Pattern(regexp = "^\\+?[0-9]{7,20}$", message = "Invalid phone number")
			String phoneNumber,

			@Schema(description = "User's email address", example = "user@example.com", nullable = true)
			@Email(message = "Invalid email format")
			@Size(max = 255)
			String email,

			@Schema(description = "User's full name", example = "John Doe", nullable = true)
			@Size(max = 255)
			String fullName,

			@Schema(description = "URL to the user's profile picture", example = "https://example.com/pic.jpg", nullable = true)
			String profilePicture,

			@Schema(description = "User's UI language", example = "en or ru", nullable = true)
			String language
	) {
	}

	@Schema(description = "DTO for assigning a role to a user")
	public record RoleAssignRequest(

			@Schema(description = "Role name to assign or remove", example = "ADMIN")
			@NotBlank
			String roleName
	) {
	}

	@Schema(description = "DTO for user location data")
	public record UserLocationRequest(

			@Schema(description = "Latitude of the user's location", example = "37.7749")
			@NotNull(message = "Latitude is required")
			Double latitude,

			@Schema(description = "Longitude of the user's location", example = "-122.4194")
			@NotNull(message = "Longitude is required")
			Double longitude,

			@Schema(description = "Accuracy of the location in meters", example = "10.5", nullable = true)
			Double accuracyMeters,

			@Schema(description = "Timestamp when location was recorded", example = "2024-01-01T12:00:00Z", nullable = true)
			OffsetDateTime recordedAt
	) {
	}

	@Schema(description = "DTO for adding an authentication method for a user")
	public record AuthMethodCreateRequest(

			@Schema(description = "Authentication provider type", example = "EMAIL_PASS")
			@NotNull(message = "Authentication type is required")
			AuthProvider authType,

			@Schema(description = "Credential data (hashed password or OAuth ID)")
			@NotBlank(message = "Credential is required")
			String credential
	) {
	}


	@Schema(description = "DTO representing user information in responses")
	public record UserResponse(

			@Schema(description = "Unique identifier of the user", example = "123e4567-e89b-12d3-a456-426614174000")
			UUID userId,

			@Schema(description = "User's phone number", example = "+1234567890", nullable = true)
			String phoneNumber,

			@Schema(description = "User's email address", example = "user@example.com", nullable = true)
			String email,

			@Schema(description = "User's full name", example = "John Doe")
			String fullName,

			@Schema(description = "URL to user's profile picture", example = "https://example.com/pic.jpg", nullable = true)
			String profilePicture,

			@Schema(description = "User's UI language", example = "en or ru", nullable = true)
			String language,

			@Schema(description = "User's status", example = "ACTIVE")
			String status,

			@Schema(description = "User's roles", example = "{\"ADMIN\", \"USER\"}")
			String roles,

			@Schema(description = "Timestamp of last login", example = "2024-01-01T12:00:00Z", nullable = true)
			OffsetDateTime lastLogin,

			@Schema(description = "User creation timestamp", example = "2023-12-31T11:00:00Z")
			OffsetDateTime createdAt,

			@Schema(description = "User last update timestamp", example = "2024-01-05T14:30:00Z")
			OffsetDateTime updatedAt
	) {
	}

}
