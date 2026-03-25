package com.tuttime.user.controller;

import com.tuttime.user.dto.*;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;
	private final AuthService authService;  // Handles auth logic, tokens, SMS, emails

	// Signup with phone or email + password
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody @Valid SignupRequest request) {
		// Delegate to service for signup logic (validation, save, send verification)
		userService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	// Signin with phone + SMS code or email + password
	@PostMapping("/signin")
	public ResponseEntity<?> signin(@RequestBody @Valid SigninRequest request) {
		String token = authService.authenticate(request);
		return ResponseEntity.ok(Map.of("token", token));
	}

	// Request SMS verification code (for signup/login)
	@PostMapping("/sms/send-code")
	public ResponseEntity<?> sendSmsCode(@RequestBody @Valid SmsVerificationRequest request) {
		authService.sendSmsVerificationCode(request.getPhoneNumber());
		return ResponseEntity.ok().build();
	}

	// Verify SMS code (login/signup confirmation)
	@PostMapping("/sms/verify-code")
	public ResponseEntity<?> verifySmsCode(@RequestBody @Valid SmsCodeVerificationRequest request) {
		authService.verifySmsCode(request.getPhoneNumber(), request.getCode());
		return ResponseEntity.ok().build();
	}

	// Request password reset email
	@PostMapping("/password/reset-request")
	public ResponseEntity<?> requestPasswordReset(@RequestBody @Valid PasswordResetRequest request) {
		authService.sendPasswordResetEmail(request.getEmail());
		return ResponseEntity.ok().build();
	}

	// Confirm password reset with token
	@PostMapping("/password/reset-confirm")
	public ResponseEntity<?> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmRequest request) {
		authService.resetPassword(request.getToken(), request.getNewPassword());
		return ResponseEntity.ok().build();
	}

	// Send email verification link
	@PostMapping("/email/send-verification")
	public ResponseEntity<?> sendEmailVerification(@RequestParam String email) {
		authService.sendEmailVerificationLink(email);
		return ResponseEntity.ok().build();
	}

	// Verify email link with token
	@GetMapping("/email/verify")
	public ResponseEntity<?> verifyEmail(@RequestParam String token) {
		authService.verifyEmailToken(token);
		return ResponseEntity.ok().build();
	}
}
