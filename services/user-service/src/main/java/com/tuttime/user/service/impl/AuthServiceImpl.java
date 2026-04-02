package com.tuttime.user.service.impl;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.*;
import com.tuttime.user.entity.PasswordResetToken;
import com.tuttime.user.entity.PhoneVerificationCode;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserAuthMethod;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.repository.PasswordResetTokenRepository;
import com.tuttime.user.repository.PhoneVerificationCodeRepository;
import com.tuttime.user.repository.UserAuthMethodRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.security.JwtTokenProvider;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.EmailService;
import com.tuttime.user.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final UserAuthMethodRepository authMethodRepository;
	private final PhoneVerificationCodeRepository phoneVerificationCodeRepository;
	private final PasswordResetTokenRepository passwordResetTokenRepository;
	private final UserAuthMethodRepository userAuthMethodRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final EmailService emailService;
	private final SmsService smsService;

	@Override
	@Transactional // CRITICAL: Ensure both saves happen in one transaction
	public void registerUser(RegisterRequest request) {
		// Validate phone/email presence
		if (!StringUtils.hasText(request.getPhoneNumber()) && !StringUtils.hasText(request.getEmail())) {
			throw new IllegalArgumentException("Either phone number or email must be provided");
		}

		// Check if user already exists by phone or email
		userRepository.findByPhoneNumberOrEmail(request.getPhoneNumber(), request.getEmail()).ifPresent(u -> {
			throw new IllegalArgumentException("User with given phone or email already exists");
		});

		// 1. Create and populate User first
		User user = new User();
		user.setPhoneNumber(request.getPhoneNumber());
		user.setEmail(request.getEmail());
		user.setFullName(request.getFullName());
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		user = userRepository.saveAndFlush(user);

		// 2. Create and link AuthMethod to the SAVED user
		UserAuthMethod authMethod = new UserAuthMethod();
		authMethod.setUserId(user.getId()); // Now 'user' has an ID!
		authMethod.setCredential(passwordEncoder.encode(request.getPassword()));
		authMethod.setAuthType(request.getEmail() != null ? AuthProvider.EMAIL_PASS : AuthProvider.PHONE_SMS);
		authMethod.setVerified(false);

		user.addAuthMethod(authMethod);

		authMethodRepository.save(authMethod);
		userRepository.save(user);

		// Send verification (email or phone)
		if (request.getEmail() != null) {
			sendEmailVerificationLink(request.getEmail());
		} else if (request.getPhoneNumber() != null) {
			sendPhoneVerificationCode(request.getPhoneNumber());
		}
	}

	@Override
	public AuthResponse loginUser(LoginRequest request) {
		String identifier = request.getEmailOrPhone();
		Optional<User> userOpt = userRepository.findByPhoneNumberOrEmail(identifier, identifier);
		if (userOpt.isEmpty()) {
			throw new SecurityException("Invalid credentials");
		}
		User user = userOpt.get();

		// Check user status
		if (user.isDeleted() || !user.getStatus().equals(UserStatus.ACTIVE)) {
			throw new SecurityException("User account is not active");
		}

		// Check auth method and verify password
		UserAuthMethod authMethod = authMethodRepository.findByUserIdAndAuthType(user.getId(),
						identifier.contains("@") ? AuthProvider.EMAIL_PASS : AuthProvider.PHONE_SMS)
				.orElseThrow(() -> new SecurityException("Authentication method not found"));

		if (!passwordEncoder.matches(request.getPassword(), authMethod.getCredential())) {
			throw new SecurityException("Invalid credentials");
		}

		// Check verification
		if (!authMethod.isVerified()) {
			throw new SecurityException("User not verified");
		}

		// Generate JWT tokens
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);

		AuthResponse auth = new AuthResponse();
		auth.setAccessToken(accessToken);
		auth.setRefreshToken(refreshToken);
		auth.setTokenType("Bearer");
		auth.setExpiresIn(jwtTokenProvider.getAccessTokenExpirySeconds());
		auth.setUserId(user.getId());
		auth.setFullName(user.getFullName());
		auth.setEmail(user.getEmail());
		auth.setPhoneNumber(user.getPhoneNumber());

		return auth;
	}

	@Override
	public AuthResponse authenticateOAuthUser(OAuthLoginRequest request) {
		// Validate provider and token (pseudo-code)
		boolean valid = validateOAuthToken(request.getProvider(), request.getOauthToken());
		if (!valid) {
			throw new SecurityException("Invalid OAuth token");
		}

		// Retrieve or create user based on OAuth ID (implementation depends on your OAuth linkage)
		User user = findOrCreateUserByOAuth(request);

		// Generate tokens
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);

		AuthResponse auth = new AuthResponse();
		auth.setAccessToken(accessToken);
		auth.setRefreshToken(refreshToken);
		auth.setTokenType("Bearer");
		auth.setExpiresIn(jwtTokenProvider.getAccessTokenExpirySeconds());
		auth.setUserId(user.getId());
		auth.setFullName(user.getFullName());
		auth.setEmail(user.getEmail());
		auth.setPhoneNumber(user.getPhoneNumber());

		return auth;
	}

	@Override
	public AuthResponse refreshJwtToken(String refreshToken) {
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new SecurityException("Invalid refresh token");
		}
		UUID userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new SecurityException("User not found"));

		String newAccessToken = jwtTokenProvider.generateAccessToken(user);
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

		AuthResponse auth = new AuthResponse();
		auth.setAccessToken(newAccessToken);
		auth.setRefreshToken(newRefreshToken);
		auth.setTokenType("Bearer");
		auth.setExpiresIn(jwtTokenProvider.getAccessTokenExpirySeconds());
		auth.setUserId(user.getId());
		auth.setFullName(user.getFullName());
		auth.setEmail(user.getEmail());
		auth.setPhoneNumber(user.getPhoneNumber());

		return auth;
	}

	@Override
	public void logoutUser(String refreshToken) {
		// Implementation depends on token invalidation strategy
		// e.g., store refresh token blacklist or remove from persistent store
		jwtTokenProvider.invalidateRefreshToken(refreshToken);
	}

	@Override
	public void sendEmailVerificationLink(String email) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("Email not registered"));
		// Generate token, save it and send email (pseudo-code)
		String token = generateVerificationToken(user);
		emailService.sendVerificationEmail(email, token);
	}

	@Override
	public void verifyEmailToken(String token) {
		User user = validateVerificationToken(token);
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);

		// Mark auth method as verified
		authMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.EMAIL_PASS)
				.ifPresent(auth -> {
					auth.setVerified(true);
					authMethodRepository.save(auth);
				});
	}

	@Override
	public void sendPhoneVerificationCode(String phoneNumber) {
		User user = userRepository.findByPhoneNumber(phoneNumber)
				.orElseThrow(() -> new IllegalArgumentException("Phone number not registered"));

		// Generate code and save
		String code = generateRandomCode();
		PhoneVerificationCode verificationCode = new PhoneVerificationCode();
		verificationCode.setPhoneNumber(phoneNumber);
		verificationCode.setCode(code);
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(300)); // 5 min expiry
		verificationCode.setUsed(false);
		phoneVerificationCodeRepository.save(verificationCode);

		// Send SMS (pseudo-code)
		smsService.sendVerificationCode(phoneNumber, code);
	}

	@Override
	public void verifyPhoneCode(String phoneNumber, String code) {
		PhoneVerificationCode verification = phoneVerificationCodeRepository
				.findTopByPhoneNumberAndCodeAndUsedFalseOrderByCreatedAtDesc(phoneNumber, code)
				.orElseThrow(() -> new IllegalArgumentException("Invalid verification code"));

		if (verification.getExpiresAt().isBefore(OffsetDateTime.now())) {
			throw new IllegalArgumentException("Verification code expired");
		}

		verification.setUsed(true);
		phoneVerificationCodeRepository.save(verification);

		// Activate user and mark auth method verified
		User user = userRepository.findByPhoneNumber(phoneNumber)
				.orElseThrow(() -> new IllegalArgumentException("User not found"));

		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);

		authMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.PHONE_SMS)
				.ifPresent(auth -> {
					auth.setVerified(true);
					authMethodRepository.save(auth);
				});
	}

	@Override
	public void initiatePasswordReset(String emailOrPhone) {
		Optional<User> userOpt = userRepository.findByEmail(emailOrPhone);
		if (userOpt.isEmpty()) {
			userOpt = userRepository.findByPhoneNumber(emailOrPhone);
		}
		User user = userOpt.orElseThrow(() -> new IllegalArgumentException("User not found"));

		if (user.getEmail() != null && user.getEmail().equalsIgnoreCase(emailOrPhone)) {
			// Email reset token flow
			String token = generatePasswordResetToken(user);
			emailService.sendPasswordResetEmail(user.getEmail(), token);
		} else if (user.getPhoneNumber() != null && user.getPhoneNumber().equals(emailOrPhone)) {
			// Phone reset code flow
			String code = generateRandomCode();
			PasswordResetToken resetToken = new PasswordResetToken();
			resetToken.setUserId(user.getId());
			resetToken.setToken(code);
			resetToken.setExpiresAt(OffsetDateTime.now().plusSeconds(600)); // 10 min expiry
			resetToken.setUsed(false);
			passwordResetTokenRepository.save(resetToken);
			smsService.sendPasswordResetCode(user.getPhoneNumber(), code);
		} else {
			throw new IllegalArgumentException("Contact method mismatch");
		}
	}

	@Override
	public void resetPassword(PasswordResetConfirmRequest request) {
		String tokenOrCode = request.getToken();
		String newPassword = request.getNewPassword();

		// Try email token reset first
		Optional<PasswordResetToken> resetTokenOpt = passwordResetTokenRepository.findByToken(tokenOrCode);
		if (resetTokenOpt.isPresent()) {
			PasswordResetToken resetToken = resetTokenOpt.get();
			if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
				throw new IllegalArgumentException("Invalid or expired reset token");
			}
			UUID userId = resetToken.getUserId();
			updatePasswordForUser(userId, newPassword);
			resetToken.setUsed(true);
			passwordResetTokenRepository.save(resetToken);
			return;
		}

		// Otherwise, check phone verification codes (treat as password reset code)
		Optional<PhoneVerificationCode> phoneCodeOpt = phoneVerificationCodeRepository.findTopByCodeAndUsedFalseOrderByCreatedAtDesc(tokenOrCode);
		if (phoneCodeOpt.isPresent()) {
			PhoneVerificationCode phoneCode = phoneCodeOpt.get();
			if (phoneCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
				throw new IllegalArgumentException("Expired verification code");
			}
			User user = userRepository.findByPhoneNumber(phoneCode.getPhoneNumber())
					.orElseThrow(() -> new IllegalArgumentException("User not found"));
			updatePasswordForUser(user.getId(), newPassword);
			phoneCode.setUsed(true);
			phoneVerificationCodeRepository.save(phoneCode);
			return;
		}

		throw new IllegalArgumentException("Invalid token or code");
	}

	@Transactional
	public AuthResponse loginWithOtp(String phoneNumber, String code) {
		// Step 1: Verify OTP
		PhoneVerificationCode otpCode = phoneVerificationCodeRepository.findTopByPhoneNumberAndCodeAndUsedFalseOrderByCreatedAtDesc(phoneNumber, code)
				.orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP code"));

		// Step 2: Mark OTP as used
		otpCode.setUsed(true);
		phoneVerificationCodeRepository.save(otpCode);

		// Step 3: Find or create user by phone number
		User user = userRepository.findByPhoneNumberAndIsDeletedFalse(phoneNumber)
				.orElseGet(() -> {
			User newUser = new User();
			newUser.setPhoneNumber(phoneNumber);
			newUser.setStatus(UserStatus.ACTIVE);
			newUser.setCreatedAt(OffsetDateTime.now());
			// other default user properties
			return userRepository.save(newUser);
		});

		// Optional: If you want to ensure the user has an auth method for PHONE_SMS
		userAuthMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.PHONE_SMS)
				.orElseGet(() -> {
			UserAuthMethod authMethod = new UserAuthMethod();
			authMethod.setUserId(user.getId());
			authMethod.setAuthType(AuthProvider.PHONE_SMS);
			authMethod.setVerified(true);
			authMethod.setCreatedAt(OffsetDateTime.now());
			return userAuthMethodRepository.save(authMethod);
		});

		// Step 4: Generate JWT tokens or whatever your AuthResponse requires
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);

		AuthResponse auth = new AuthResponse();
		auth.setAccessToken(accessToken);
		auth.setRefreshToken(refreshToken);
		auth.setTokenType("Bearer");
		auth.setExpiresIn(jwtTokenProvider.getAccessTokenExpirySeconds());
		auth.setUserId(user.getId());
		auth.setFullName(user.getFullName());
		auth.setEmail(user.getEmail());
		auth.setPhoneNumber(user.getPhoneNumber());

		return auth;
	}

	private void updatePasswordForUser(UUID userId, String newPassword) {
		UserAuthMethod authMethod = authMethodRepository.findByUserIdAndAuthType(userId, AuthProvider.EMAIL_PASS)
				.orElseThrow(() -> new IllegalArgumentException("Authentication method not found"));
		authMethod.setCredential(passwordEncoder.encode(newPassword));
		authMethod.setVerified(true);
		authMethodRepository.save(authMethod);
	}

	// Helper stubs - implement these appropriately

	private boolean validateOAuthToken(String provider, String oauthToken) {
		// Validate with external OAuth provider
		return true;
	}

	private User findOrCreateUserByOAuth(OAuthLoginRequest request) {
		// Find or create user based on OAuth provider data
		return new User(); // stub
	}

	private String generateVerificationToken(User user) {
		// Generate and persist email verification token
		return UUID.randomUUID().toString();
	}

	private User validateVerificationToken(String token) {
		// 1. Find the token in the database
		PasswordResetToken verificationToken = passwordResetTokenRepository.findByToken(token)
				.orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

		// 2. Check if the token has expired
		if (verificationToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
			throw new IllegalArgumentException("Verification code expired");
		}

		// 3. Return the fully loaded user entity from the database
		return userRepository.findById(verificationToken.getUserId())
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private String generateRandomCode() {
		// Generate numeric code (e.g., 6-digit)
		return "123456";
	}

	private String generatePasswordResetToken(User user) {
		// Generate and persist password reset token
		return UUID.randomUUID().toString();
	}
}