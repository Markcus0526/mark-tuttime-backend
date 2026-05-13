package com.tuttime.user.dto;

import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.user.entity.enums.DevicePlatform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.UUID;

public class AuthDto {

	@Schema(description = "DTO for verifying phone number via verification code")
	public record OtpVerificationRequest(
			@Schema(description = "Phone number in E.164 format", example = "+1234567890")
			@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
			String phoneNumber,

			@Schema(description = "Email address", example = "user@example.com")
			@Email(message = "Invalid email")
			String email
	) {
	}

	@Schema(description = "DTO for verifying phone number via verification code")
	public record OtpVerificationConfirmRequest(

			@Schema(description = "Phone number in E.164 format", example = "+1234567890")
			@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
			String phoneNumber,

			@Schema(description = "Email address", example = "user@example.com")
			@Email(message = "Invalid email")
			String email,

			@Schema(description = "Verification code received via SMS")
			@NotBlank
			String code
	) {
	}

	@Schema(description = "DTO to request a password reset token by email")
	public record PasswordResetRequest(
			@Schema(description = "Email address", example = "user@example.com")
			@Email(message = "Invalid email")
			String email
	) {
	}

	@Schema(description = "DTO to confirm and complete password reset")
	public record PasswordResetConfirmRequest(

			@Schema(description = "Email address", example = "user@example.com")
			@Email(message = "Invalid email")
			String email,

			@Schema(description = "Password reset OTP code")
			@NotBlank(message = "Reset OTP code is required")
			String code,

			@Schema(description = "New password for the user", minLength = 8)
			@NotBlank(message = "New password is required")
			@Size(min = 8, message = "Password must be at least 8 characters")
			String newPassword
	) {
	}

	public record TokenRefreshRequest(
			@Schema(description = "Refresh token to obtain new access token")
			@NotBlank
			String refreshToken
	) {
	}

	@Schema(description = "Login request details")
	public record LoginRequest(
			@Schema(description = "Email address or phone number for login", example = "user@example.com or +1234567890")
			@NotBlank(message = "Email or phone is required")
			String emailOrPhone,

			@Schema(description = "User password", example = "StrongPassword123!")
			@NotBlank(message = "Password is required")
			String password,

			DeviceRegistrationRequest deviceInfo
	) {
	}

	public record OAuthLoginRequest(
			@Schema(description = "OAuth provider name (e.g., GOOGLE, APPLE)", example = "GOOGLE")
			@NotNull(message = "Provider is required")
			AuthProvider provider,

			@Schema(description = "OAuth access token from provider")
			@NotBlank
			String oauthToken
	) {
	}

	@Schema(description = "Login request details")
	public record LogoutRequest(
			@Schema(description = "Refresh token to invalidate")
			@NotBlank
			String refreshToken,

			String fcmToken
	) {
	}

	@Schema(description = "Register request details")
	public record RegisterRequest(
			@Schema(description = "Phone number in E.164 format", example = "+79164567890", nullable = true)
			@Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
			String phoneNumber,

			@Schema(description = "Email address", example = "user@example.com", nullable = true)
			@Email(message = "Invalid email")
			String email,

			@Schema(description = "User password", example = "12345678")
			// @NotBlank(message = "Password is required")
			String password,

			@Schema(description = "User's UI language", example = "en or ru", nullable = true)
			String language
	) {
	}

	public record DeviceRegistrationRequest(
			@NotBlank String token,
			@NotNull DevicePlatform platform,
			String deviceName
	) {
	}

	public record AuthResponse(
			@Schema(description = "JWT access token")
			String accessToken,

			@Schema(description = "JWT refresh token")
			String refreshToken,

			@Schema(description = "Token type (usually Bearer)", example = "Bearer")
			String tokenType,

			@Schema(description = "Access token expiration time in seconds", example = "3600")
			long expiresIn,

			@Schema(description = "User ID")
			UUID userId,

			@Schema(description = "User roles", example = "['OWNER', 'MANAGER']")
			java.util.List<String> roles,// Added: Essential for frontend routing

			@Schema(description = "Restaurant/Branch management details", nullable = true)
			RestaurantStaffDto staff, // Added: Tells the app which entity they manage

			@Schema(description = "Full name of the user")
			String fullName,

			@Schema(description = "User email address", nullable = true)
			String email,

			@Schema(description = "User phone number", nullable = true)
			String phoneNumber,

			@Schema(description = "User profile url", nullable = true)
			String profilePicture,

			@Schema(description = "User's UI language", example = "en or ru", nullable = true)
			String language
	) {
		// Nested Record for immutability and cleaner code
		public record RestaurantStaffDto(
				UUID restaurantId,
				UUID branchId,
				String entityName,
				boolean active
		) {
		}

		// Using a Java Record (cleanest way)
		public record OAuthUserInfo(
				String providerId, // The unique ID from Google/Apple (usually 'sub')
				String email,      // The user's email
				String fullName        // Optional: user's name
		) {
		}
	}

}
