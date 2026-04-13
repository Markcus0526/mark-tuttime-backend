package com.tuttime.user.service.impl;

import com.tuttime.common.auth.enums.UserRoleKind;
import com.tuttime.common.exception.AuthenticationException;
import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.*;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.user.entity.enums.UserStatus;
import com.tuttime.user.entity.enums.VerificationType;
import com.tuttime.user.repository.*;
import com.tuttime.user.security.JwtTokenProvider;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.EmailService;
import com.tuttime.user.service.SmsService;
import com.tuttime.user.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

	private final UserRepository userRepository;
	private final VerificationCodeRepository verificationCodeRepository;
	private final UserAuthMethodRepository userAuthMethodRepository;
	private final UserRoleRepository userRoleRepository;
	private final RoleRepository roleRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final EmailService emailService;
	private final SmsService smsService;
	private final TokenUtil tokenUtil;

	@Override
	@Transactional // CRITICAL: Ensure both saves happen in one transaction
	public void registerUser(RegisterRequest request) {
		String phoneNumber = request.getPhoneNumber();
		String email = request.getEmail();
		String password = request.getPassword();

		// Validate phone/email presence
		if (!StringUtils.hasText(phoneNumber) && !StringUtils.hasText(email)) {
			throw new MethodArgumentNotValidException("Either phone number or email must be provided");
		}

		if (!StringUtils.hasText(phoneNumber) && StringUtils.hasText(email) && !StringUtils.hasText(password)) {
			throw new MethodArgumentNotValidException("Password must be provided");
		}

		// Check if user already exists by phone or email
		if (StringUtils.hasText(request.getPhoneNumber()) &&
				userRepository.existsByPhoneNumberAndDeletedFalse(request.getPhoneNumber())) {
			throw new ConflictException("Phone number in use");
		}

		if (StringUtils.hasText(request.getEmail()) &&
				userRepository.existsByEmailAndDeletedFalse(request.getEmail())) {
			throw new ConflictException("Email in use");
		}

		// 1. Create and populate User first
		User user = new User();
		user.setPhoneNumber(phoneNumber);
		user.setEmail(email);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		user = userRepository.saveAndFlush(user);

		String targetRole = UserRoleKind.USER.name();
		Role userRole = roleRepository.findByName(UserRoleKind.USER.name())
				.orElseThrow(() -> new ResourceNotFoundException("Role " + targetRole + " not found"));
		UserRole ur = new UserRole();
		ur.setUserId(user.getUserId());
		ur.setRole(userRole);
		userRoleRepository.save(ur);

		// 2. Create and link AuthMethod to the SAVED user
		UserAuthMethod authMethod = new UserAuthMethod();
		authMethod.setUserId(user.getUserId()); // Now 'user' has an ID!
		authMethod.setCredential(passwordEncoder.encode(request.getPassword()));
		authMethod.setAuthType(StringUtils.hasText(phoneNumber) ? AuthProvider.PHONE_SMS : AuthProvider.EMAIL_PASS);
		authMethod.setVerified(false);

		user.addAuthMethod(authMethod);

		userAuthMethodRepository.save(authMethod);
		userRepository.save(user);

		// Send verification (email or phone)
		OtpVerificationRequest optRequest = new OtpVerificationRequest();
		optRequest.setPhoneNumber(phoneNumber);
		optRequest.setEmail(email);
		sendOtpVerificationCode(optRequest);
	}

	@Override
	public void loginUser(LoginRequest request) {
		String identifier = request.getEmailOrPhone();
		String password = request.getPassword();

		if (!StringUtils.hasText(identifier)) {
			throw new MethodArgumentNotValidException("Either phone number or email must be provided");
		}

		String phoneNumber = null;
		String email =  null;
		if (identifier.contains("@"))
			email = identifier;
		else
			phoneNumber = identifier;

		if (!StringUtils.hasText(phoneNumber) && StringUtils.hasText(email) && !StringUtils.hasText(password)) {
			throw new MethodArgumentNotValidException("Password must be provided");
		}

		Optional<User> userOpt = userRepository.findByPhoneNumberOrEmail(phoneNumber, email);
		if (userOpt.isEmpty()) {
			throw new MethodArgumentNotValidException("Either phone number or email not registered");
		}

		User user = userOpt.get();

		// Check user status
		if (!user.getStatus().equals(UserStatus.ACTIVE)) {
			throw new AuthenticationException("User account is not active");
		}

		// Check auth method and verify password
		if (StringUtils.hasText(email)) {
			UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getUserId(), AuthProvider.EMAIL_PASS)
					.orElseThrow(() -> new SecurityException("Authentication method not found"));

			if (!passwordEncoder.matches(request.getPassword(), authMethod.getCredential())) {
				throw new AuthenticationException("Invalid credentials");
			}

			// Check verification
			if (!authMethod.isVerified()) {
				throw new AuthenticationException("User not verified");
			}
		}

		// Send verification (email or phone)
		OtpVerificationRequest optRequest = new OtpVerificationRequest();
		optRequest.setPhoneNumber(phoneNumber);
		optRequest.setEmail(email);
		sendOtpVerificationCode(optRequest);
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
		auth.setUserId(user.getUserId());
		auth.setFullName(user.getFullName());
		auth.setEmail(user.getEmail());
		auth.setPhoneNumber(user.getPhoneNumber());

		return auth;
	}

	@Override
	public AuthResponse refreshJwtToken(String refreshToken) {
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new AuthenticationException("Invalid refresh token");
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
		auth.setUserId(user.getUserId());
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
	public void sendOtpVerificationCode(OtpVerificationRequest request) {
		String phoneNumber = request.getPhoneNumber();
		String email = request.getEmail();

		// Validate phone/email presence
		if (!StringUtils.hasText(phoneNumber) && !StringUtils.hasText(email)) {
			throw new MethodArgumentNotValidException("Either phone number or email must be provided");
		}

		// Check if user already exists by phone or email
		if (StringUtils.hasText(phoneNumber) &&
				!userRepository.existsByPhoneNumberAndDeletedFalse(phoneNumber)) {
			throw new ResourceNotFoundException("Phone number not registered");
		}

		if (StringUtils.hasText(email) &&
				!userRepository.existsByEmailAndDeletedFalse(email)) {
			throw new ResourceNotFoundException("Email not registered");
		}

		// Generate code and save
		String code = tokenUtil.generateRandomCode();
		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(StringUtils.hasText(phoneNumber) ? VerificationType.PHONE_VERIFY : VerificationType.EMAIL_VERIFY);
		verificationCode.setTarget(StringUtils.hasText(phoneNumber) ? phoneNumber : email);
		verificationCode.setCodeHash(code);
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(300)); // 5 min expiry
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);

		if (StringUtils.hasText(phoneNumber)) {
			smsService.sendVerificationCode(phoneNumber, code);
		} else {
			emailService.sendVerificationEmail(email, code);
		}
	}

	@Transactional
	public AuthResponse loginWithOtp(OtpVerificationConfirmRequest request) {
		String phoneNumber = request.getPhoneNumber();
		String email = request.getEmail();
		String code = request.getCode();

		// Validate phone/email presence
		if (!StringUtils.hasText(phoneNumber) && !StringUtils.hasText(email)) {
			throw new MethodArgumentNotValidException("Either phone number or email must be provided");
		}
		if (!StringUtils.hasText(code)) {
			throw new MethodArgumentNotValidException("OTP code must be provided");
		}

		String target = StringUtils.hasText(phoneNumber) ? phoneNumber : email;
		// Step 1: Verify OTP
		VerificationCode otpCode = verificationCodeRepository.findTopByTargetAndCodeHashAndUsedFalseOrderByCreatedAtDesc(target, code)
				.orElseThrow(() -> new MethodArgumentNotValidException("Invalid or expired OTP code"));

		// Step 2: Mark OTP as used
		otpCode.setUsed(true);
		verificationCodeRepository.save(otpCode);

		// Step 3: Find or create user by phone number
		User user;
		if (otpCode.getType() == VerificationType.PHONE_VERIFY) {
			user = userRepository.findByPhoneNumberAndDeletedFalse(phoneNumber)
					.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		} else {
			user = userRepository.findByEmailAndDeletedFalse(email)
					.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		}

		user.setStatus(UserStatus.ACTIVE);
		user.setLastLogin(OffsetDateTime.now());
		userRepository.save(user);

		// Optional: If you want to ensure the user has an auth method for PHONE_SMS
		if (otpCode.getType() == VerificationType.EMAIL_VERIFY) {
			UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getUserId(), AuthProvider.EMAIL_PASS)
					.orElseGet(() -> {
						UserAuthMethod newAuthMethod = new UserAuthMethod();
						newAuthMethod.setUserId(user.getUserId());
						newAuthMethod.setAuthType(AuthProvider.EMAIL_PASS);
						newAuthMethod.setVerified(true);
						return userAuthMethodRepository.save(newAuthMethod);
					});
			authMethod.setVerified(true);
		}

		// Step 4: Generate JWT tokens or whatever your AuthResponse requires
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);

		AuthResponse auth = new AuthResponse();
		auth.setAccessToken(accessToken);
		auth.setRefreshToken(refreshToken);
		auth.setTokenType("Bearer");
		auth.setExpiresIn(jwtTokenProvider.getAccessTokenExpirySeconds());
		auth.setUserId(user.getUserId());
		auth.setFullName(user.getFullName());
		auth.setEmail(user.getEmail());
		auth.setPhoneNumber(user.getPhoneNumber());

		return auth;
	}

	@Override
	public void initiatePasswordReset(PasswordResetRequest request) {
		String email = request.getEmail();

		if (!StringUtils.hasText(email)) {
			throw new ResourceNotFoundException("Email must be provided");
		}

		Optional<User> userOpt = userRepository.findByEmailAndDeletedFalse(email);
		User user = userOpt.orElseThrow(() -> new IllegalArgumentException("User not found"));

		// Generate code and save
		String code = tokenUtil.generateRandomCode();
		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(VerificationType.EMAIL_VERIFY);
		verificationCode.setTarget(email);
		verificationCode.setCodeHash(code);
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(300)); // 5 min expiry
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);
		emailService.sendVerificationEmail(email, code);
	}

	@Override
	public void resetPassword(PasswordResetConfirmRequest request) {
		String email = request.getEmail();
		String newPassword = request.getNewPassword();
		String code = request.getCode();

		// Validate phone/email presence
		if (!StringUtils.hasText(email)) {
			throw new MethodArgumentNotValidException("Email must be provided");
		}
		if (!StringUtils.hasText(newPassword)) {
			throw new MethodArgumentNotValidException("Password must be provided");
		}
		if (!StringUtils.hasText(code)) {
			throw new MethodArgumentNotValidException("OTP code must be provided");
		}

		// Step 1: Verify OTP
		VerificationCode otpCode = verificationCodeRepository.findTopByTargetAndCodeHashAndUsedFalseOrderByCreatedAtDesc(email, code)
				.orElseThrow(() -> new IllegalArgumentException("Invalid or expired OTP code"));

		// Step 2: Mark OTP as used
		otpCode.setUsed(true);
		verificationCodeRepository.save(otpCode);

		// Step 3: Find or create user by phone number
		User user = userRepository.findByEmailAndDeletedFalse(email)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));

		updatePasswordForUser(user.getUserId(), newPassword);
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
	}

	private void updatePasswordForUser(UUID userId, String newPassword) {
		UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(userId, AuthProvider.EMAIL_PASS)
				.orElseThrow(() -> new IllegalArgumentException("Authentication method not found"));
		authMethod.setCredential(passwordEncoder.encode(newPassword));
		authMethod.setVerified(true);
		userAuthMethodRepository.save(authMethod);
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
}