package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.UUID;

@Data
public class AuthResponse {
	@Schema(description = "JWT access token")
	String accessToken;

	@Schema(description = "JWT refresh token")
	String refreshToken;

	@Schema(description = "Token type (usually Bearer)", example = "Bearer")
	String tokenType;

	@Schema(description = "Access token expiration time in seconds", example = "3600")
	long expiresIn;

	@Schema(description = "User ID")
	UUID userId;

	@Schema(description = "User roles", example = "['OWNER', 'MANAGER']")
	java.util.List<String> roles; // Added: Essential for frontend routing

	@Schema(description = "Restaurant/Branch management details", nullable = true)
	RestaurantStaff staff; // Added: Tells the app which entity they manage

	@Schema(description = "Full name of the user")
	String fullName;

	@Schema(description = "User email address", nullable = true)
	String email;

	@Schema(description = "User phone number", nullable = true)
	String phoneNumber;

	@Data
	public static class RestaurantStaff {
		private UUID restaurantId;
		private UUID branchId;     // Null if the user is a Restaurant Owner
		private String entityName; // e.g., "Mamma Mia Trattoria"
		private boolean active;
	}
}

