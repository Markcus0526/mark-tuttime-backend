package com.tuttime.user.service;

import com.tuttime.user.dto.*;

/**
 * Service interface for handling user authentication, registration,
 * verification, password reset, and token management.
 */
public interface AuthService {

	/**
	 * Registers a new user with provided details.
	 * Sends verification email or phone code based on contact method.
	 *
	 * @param request Registration request DTO containing user info
	 * @throws IllegalArgumentException if validation fails or user exists
	 */
	void registerUser(RegisterRequest request);

	/**
	 * Authenticates user by email/phone and password.
	 * Only verified users can authenticate.
	 *
	 * @param request Login request DTO with emailOrPhone and password
	 * @throws SecurityException if authentication fails or user not verified
	 */
	void loginUser(LoginRequest request);

	/**
	 * Authenticates user via OAuth provider token.
	 * Assumes OAuth provider has confirmed user identity.
	 *
	 * @param request OAuth login request DTO
	 * @return AuthResponseDto containing JWT tokens and user info
	 * @throws SecurityException if OAuth token is invalid
	 */
	AuthResponse authenticateOAuthUser(OAuthLoginRequest request);

	/**
	 * Refreshes JWT access token using a valid refresh token.
	 *
	 * @param refreshToken Refresh token string
	 * @return AuthResponseDto with new access and refresh tokens
	 * @throws SecurityException if refresh token is invalid or expired
	 */
	AuthResponse refreshJwtToken(String refreshToken);

	/**
	 * Logs out a user by invalidating the provided refresh token.
	 *
	 * @param refreshToken Refresh token to invalidate
	 */
	void logoutUser(String refreshToken);

	/**
	 * Sends a phone verification code via SMS to the specified phone number.
	 *
	 * @param request to verify
	 */
	void sendOtpVerificationCode(OtpVerificationRequest request);

	/**
	 * Initiates password reset by sending an email or SMS code depending on user contact.
	 *
	 * @param PasswordResetRequest request
	 */
	void initiatePasswordReset(PasswordResetRequest request);

	/**
	 * Resets the user's password using a token (email) or code (phone).
	 *
	 * @param request Password reset confirmation DTO containing token/code and new password
	 * @throws IllegalArgumentException if token/code is invalid or expired
	 */
	void resetPassword(PasswordResetConfirmRequest request);

	AuthResponse loginWithOtp(OtpVerificationConfirmRequest request);
}
