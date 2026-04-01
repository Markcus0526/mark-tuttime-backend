package com.tuttime.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
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

	@Schema(description = "Full name of the user")
	String fullName;

	@Schema(description = "User email address", nullable = true)
	String email;

	@Schema(description = "User phone number", nullable = true)
	String phoneNumber;
}

