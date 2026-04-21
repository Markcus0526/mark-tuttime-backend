package com.tuttime.user.controller;

import com.tuttime.common.dto.ApiRes;
import com.tuttime.user.dto.AuthDto;
import com.tuttime.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
@Tag(name = "User Authorization", description = "Operations for registering, sms, email, Oauth, login, logout, and verification")
public class AuthController {

	private final AuthService authService;

	@Operation(summary = "Register a new user (email or phone). Sends verification accordingly.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "User registered successfully, verification sent"),
			@ApiResponse(responseCode = "400", description = "Invalid request data", content = @Content)
	})
	@PostMapping("/register")
	public ResponseEntity<ApiRes<Void>> register(
			@Parameter(description = "Registration request with email or phone", required = true)
			@Valid @RequestBody AuthDto.RegisterRequest request) {

		authService.registerUser(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success("New user registered successfully", null));
	}

	@Operation(summary = "Login user with email or phone and password.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Login successful"),
			@ApiResponse(responseCode = "401", description = "Invalid credentials or account not verified", content = @Content)
	})
	@PostMapping("/login")
	public ResponseEntity<ApiRes<Void>> login(
			@Parameter(description = "Login request containing email or phone and password", required = true)
			@Valid @RequestBody AuthDto.LoginRequest request) {

		authService.loginUser(request);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Logout user by invalidating refresh token.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Logout successful"),
			@ApiResponse(responseCode = "400", description = "Invalid request", content = @Content)
	})
	@PostMapping("/logout")
	public ResponseEntity<ApiRes<Void>> logout(
			@Parameter(description = "Logout request with refresh token", required = true)
			@Valid @RequestBody AuthDto.LogoutRequest request) {

		authService.logoutUser(request.refreshToken());
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Login user via OAuth providers (Google, Apple, etc.)")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OAuth login successful"),
			@ApiResponse(responseCode = "401", description = "Invalid OAuth token", content = @Content)
	})
	@PostMapping("/oauth-login")
	public ResponseEntity<ApiRes<AuthDto.AuthResponse>> oauthLogin(
			@Parameter(description = "OAuth login request with provider and token", required = true)
			@Valid @RequestBody AuthDto.OAuthLoginRequest request) {

		AuthDto.AuthResponse response = authService.authenticateOAuthUser(request);
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@Operation(summary = "Refresh JWT access token using refresh token.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
			@ApiResponse(responseCode = "401", description = "Invalid or expired refresh token", content = @Content)
	})
	@PostMapping("/refresh-token")
	public ResponseEntity<ApiRes<AuthDto.AuthResponse>> refreshToken(
			@Parameter(description = "Token refresh request with refresh token", required = true)
			@Valid @RequestBody AuthDto.TokenRefreshRequest request) {

		AuthDto.AuthResponse response = authService.refreshJwtToken(request.refreshToken());
		return ResponseEntity.ok(ApiRes.success(response));
	}

	@Operation(summary = "Request password reset. Sends email or phone reset depending on contact.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Password reset initiated"),
			@ApiResponse(responseCode = "400", description = "Invalid request or contact not found", content = @Content)
	})
	@PostMapping("/password-reset/request")
	public ResponseEntity<ApiRes<Void>> requestPasswordReset(
			@Parameter(description = "Request with email or phone to initiate password reset", required = true)
			@Valid @RequestBody AuthDto.PasswordResetRequest request) {

		authService.initiatePasswordReset(request);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Confirm password reset using token (email) or code (phone).")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "Password reset successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid or expired token/code", content = @Content)
	})
	@PostMapping("/password-reset/confirm")
	public ResponseEntity<ApiRes<Void>> confirmPasswordReset(
			@Parameter(description = "Password reset confirmation request", required = true)
			@Valid @RequestBody AuthDto.PasswordResetConfirmRequest request) {

		authService.resetPassword(request);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Request OTP login code via SMS for phone number.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "204", description = "OTP code sent to phone"),
			@ApiResponse(responseCode = "400", description = "Invalid phone number", content = @Content)
	})
	@PostMapping("/otp/request")
	public ResponseEntity<ApiRes<Void>> requestOtpLogin(
			@Parameter(description = "Phone number to receive OTP code", required = true)
			@Valid @RequestBody AuthDto.OtpVerificationRequest request) {

		authService.sendOtpVerificationCode(request);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
	}

	@Operation(summary = "Verify OTP code and login with phone number.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "OTP verified and login successful"),
			@ApiResponse(responseCode = "400", description = "Invalid or expired OTP code", content = @Content)
	})
	@PostMapping("/otp/verify")
	public ResponseEntity<ApiRes<AuthDto.AuthResponse>> verifyOtpLogin(
			@Parameter(description = "Phone number or email and OTP code for login", required = true)
			@Valid @RequestBody AuthDto.OtpVerificationConfirmRequest request) {

		AuthDto.AuthResponse response = authService.loginWithOtp(request);
		return ResponseEntity.ok(ApiRes.success(response));
	}
}
