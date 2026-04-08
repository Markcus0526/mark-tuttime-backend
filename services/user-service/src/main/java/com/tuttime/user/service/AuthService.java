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
	 * @return AuthResponseDto containing JWT tokens and user info
	 * @throws SecurityException if authentication fails or user not verified
	 */
	AuthResponse loginUser(LoginRequest request);

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
	 * Sends an email verification link to the specified email address.
	 *
	 * @param email User's email address to verify
	 */
	void sendEmailVerificationLink(String email);

	/**
	 * Verifies email account using the token received from email link.
	 *
	 * @param token Email verification token
	 * @throws IllegalArgumentException if token is invalid or expired
	 */
	void verifyEmailToken(String token);

	/**
	 * Sends a phone verification code via SMS to the specified phone number.
	 *
	 * @param request to verify
	 */
	void sendOtpVerificationCode(OtpVerificationRequest request);

	/**
	 * Verifies a phone number using the received SMS verification code.
	 *
	 * @param phoneNumber Phone number being verified
	 * @param code        Verification code received via SMS
	 * @throws IllegalArgumentException if code is invalid or expired
	 */
	void verifyPhoneCode(String phoneNumber, String code);

	/**
	 * Initiates password reset by sending an email or SMS code depending on user contact.
	 *
	 * @param emailOrPhone Email or phone number of the user
	 */
	void initiatePasswordReset(String emailOrPhone);

	/**
	 * Resets the user's password using a token (email) or code (phone).
	 *
	 * @param request Password reset confirmation DTO containing token/code and new password
	 * @throws IllegalArgumentException if token/code is invalid or expired
	 */
	void resetPassword(PasswordResetConfirmRequest request);

	AuthResponse loginWithOtp(OtpVerificationConfirmRequest request);
}
