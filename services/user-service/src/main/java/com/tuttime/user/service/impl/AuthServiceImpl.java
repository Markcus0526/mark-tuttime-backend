package com.tuttime.user.service.impl;

import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.event.proto.StaffUserRequest;
import com.tuttime.common.event.proto.StaffUserResponse;
import com.tuttime.user.dto.AuthDto;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.common.enums.UserStatus;
import com.tuttime.user.entity.enums.VerificationType;
import com.tuttime.user.grpc.client.RestaurantGrpcClient;
import com.tuttime.user.mapper.AuthMapper;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
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
	private final AuthMapper authMapper;
	private final TokenUtil tokenUtil;
	private final RestaurantGrpcClient restaurantGrpcClient;

	@Override
	@Transactional // CRITICAL: Ensure both saves happen in one transaction
	public void registerUser(AuthDto.RegisterRequest request) {
		String phoneNumber = request.phoneNumber();
		String email = request.email();
		String password = request.password();

		// Validate phone/email presence
		if (!StringUtils.hasText(phoneNumber) && !StringUtils.hasText(email)) {
			throw new InvalidRequestException("Either phone number or email must be provided");
		}

		if (!StringUtils.hasText(phoneNumber) && StringUtils.hasText(email) && !StringUtils.hasText(password)) {
			throw new InvalidRequestException("Password must be provided");
		}

		// Check if user already exists by phone or email
		if (StringUtils.hasText(request.phoneNumber()) &&
				userRepository.existsByPhoneNumberAndDeletedFalse(request.phoneNumber())) {
			throw new InvalidRequestException("Phone number in use");
		}

		if (StringUtils.hasText(request.email()) &&
				userRepository.existsByEmailAndDeletedFalse(request.email())) {
			throw new InvalidRequestException("Email in use");
		}

		// 1. Create and populate User first
		User user = new User();
		user.setPhoneNumber(phoneNumber);
		user.setEmail(email);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		user = userRepository.saveAndFlush(user);

		String targetRole = UserRoleKind.USER.name();
		Role role = roleRepository.findByName(UserRoleKind.USER.name())
				.orElseThrow(() -> new InvalidRequestException("Role " + targetRole + " not found"));
		UserRole userRole = UserRole.builder()
				.user(user)
				.role(role)
				.build();
		userRoleRepository.save(userRole);

		// 2. Create and link AuthMethod to the SAVED user
		UserAuthMethod authMethod = new UserAuthMethod();
		authMethod.setUser(user); // Now 'user' has an ID!
		authMethod.setCredential(passwordEncoder.encode(request.password()));
		authMethod.setAuthType(StringUtils.hasText(phoneNumber) ? AuthProvider.PHONE_SMS : AuthProvider.EMAIL_PASS);
		authMethod.setVerified(false);

		user.addAuthMethod(authMethod);

		userAuthMethodRepository.save(authMethod);
		userRepository.save(user);

		// Send verification (email or phone)
		AuthDto.OtpVerificationRequest optRequest = new AuthDto.OtpVerificationRequest(phoneNumber, email);
		sendOtpVerificationCode(optRequest);
	}

	@Override
	@Transactional
	public void loginUser(AuthDto.LoginRequest request) {
		String identifier = request.emailOrPhone();
		String password = request.password();

		if (!StringUtils.hasText(identifier)) {
			throw new InvalidRequestException("Either phone number or email must be provided");
		}

		String phoneNumber = null;
		String email = null;
		if (identifier.contains("@"))
			email = identifier;
		else
			phoneNumber = identifier;

		if (!StringUtils.hasText(phoneNumber) && StringUtils.hasText(email) && !StringUtils.hasText(password)) {
			throw new InvalidRequestException("Password must be provided");
		}

		Optional<User> userOpt = userRepository.findByPhoneNumberOrEmail(phoneNumber, email);
		if (userOpt.isEmpty()) {
			throw new InvalidRequestException("Either phone number or email not registered");
		}

		User user = userOpt.get();

		// Check user status
		if (!user.getStatus().equals(UserStatus.ACTIVE)) {
			throw new ForbiddenException("User account is not active");
		}

		// Check auth method and verify password
		if (StringUtils.hasText(email)) {
			UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.EMAIL_PASS)
					.orElseThrow(() -> new ForbiddenException("Authentication method not found"));

			if (!passwordEncoder.matches(request.password(), authMethod.getCredential())) {
				throw new ForbiddenException("Invalid credentials");
			}

			// Check verification
			if (!authMethod.isVerified()) {
				throw new ForbiddenException("User not verified");
			}
		}

		// Send verification (email or phone)
		AuthDto.OtpVerificationRequest optRequest = new AuthDto.OtpVerificationRequest(phoneNumber, email);
		sendOtpVerificationCode(optRequest);
	}

	@Override
	public AuthDto.AuthResponse authenticateOAuthUser(AuthDto.OAuthLoginRequest request) {
		// Validate provider and token (pseudo-code)
		boolean valid = validateOAuthToken(request.provider(), request.oauthToken());
		if (!valid) {
			throw new ForbiddenException("Invalid OAuth token");
		}

		// Retrieve or create user based on OAuth ID (implementation depends on your OAuth linkage)
		User user = findOrCreateUserByOAuth(request);

		// Generate tokens
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);

		return authMapper.toAuthResponse(
				user,
				accessToken,
				refreshToken,
				jwtTokenProvider.getAccessTokenExpirySeconds(),
				null);
	}

	@Override
	public AuthDto.AuthResponse refreshJwtToken(String refreshToken) {
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new ForbiddenException("Invalid refresh token");
		}
		UUID userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		String newAccessToken = jwtTokenProvider.generateAccessToken(user);
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

		return authMapper.toAuthResponse(
				user,
				newAccessToken,
				newRefreshToken,
				jwtTokenProvider.getAccessTokenExpirySeconds(),
				null);
	}

	@Override
	public void logoutUser(String refreshToken) {
		// Implementation depends on token invalidation strategy
		// e.g., store refresh token blacklist or remove from persistent store
		jwtTokenProvider.invalidateRefreshToken(refreshToken);
	}

	@Override
	@Transactional
	public void sendOtpVerificationCode(AuthDto.OtpVerificationRequest request) {
		String phoneNumber = request.phoneNumber();
		String email = request.email();

		// Validate phone/email presence
		if (!StringUtils.hasText(phoneNumber) && !StringUtils.hasText(email)) {
			throw new InvalidRequestException("Either phone number or email must be provided");
		}

		// Check if user already exists by phone or email
		if (StringUtils.hasText(phoneNumber) &&
				!userRepository.existsByPhoneNumberAndDeletedFalse(phoneNumber)) {
			throw new InvalidRequestException("Phone number not registered");
		}

		if (StringUtils.hasText(email) &&
				!userRepository.existsByEmailAndDeletedFalse(email)) {
			throw new InvalidRequestException("Email not registered");
		}

		// Generate code and save
		String code = tokenUtil.generateRandomCode();
		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(StringUtils.hasText(phoneNumber) ? VerificationType.PHONE_VERIFY : VerificationType.EMAIL_VERIFY);
		verificationCode.setTarget(StringUtils.hasText(phoneNumber) ? phoneNumber : email);
		verificationCode.setCodeHash(code);
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(300)); // 5 min expiry
		verificationCode.setUsed(false);
		verificationCodeRepository.saveAndFlush(verificationCode);

		if (StringUtils.hasText(phoneNumber)) {
			smsService.sendVerificationCode(phoneNumber, code);
		} else {
			emailService.sendVerificationEmail(email, code);
		}
	}

	@Transactional
	public AuthDto.AuthResponse loginWithOtp(AuthDto.OtpVerificationConfirmRequest request) {
		String target = StringUtils.hasText(request.phoneNumber()) ? request.phoneNumber() : request.email();

		VerificationCode otpCode = verificationCodeRepository.findTopByTargetAndCodeHashAndUsedFalseOrderByCreatedAtDesc(target, request.code())
				.orElseThrow(() -> new InvalidRequestException("Invalid or expired OTP code"));

		otpCode.setUsed(true);
		verificationCodeRepository.save(otpCode);

		// 2. Find User
		User user = (otpCode.getType() == VerificationType.PHONE_VERIFY)
				? userRepository.findByPhoneNumberAndDeletedFalse(target)
				.orElseThrow(() -> new InvalidRequestException("User not found"))
				: userRepository.findByEmailAndDeletedFalse(target)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		// 3. Update User State
		user.setStatus(UserStatus.ACTIVE);
		user.setLastLogin(OffsetDateTime.now());
		userRepository.save(user);

		// Optional: If you want to ensure the user has an auth method for EMAIL_PASS
		if (otpCode.getType() == VerificationType.EMAIL_VERIFY) {
			UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.EMAIL_PASS)
					.orElseThrow(() -> new InvalidRequestException("Authentication method not found"));
			authMethod.setVerified(true);
			userAuthMethodRepository.save(authMethod);
		}

		// 4. Fetch Management Context (gRPC) if applicable
		AuthDto.AuthResponse.RestaurantStaff staffContext = null;
		List<String> roleNames = user.getRoles().stream()
				.map(ur -> ur.getRole().getName())
				.toList();

		if (roleNames.stream().anyMatch(role -> List.of("OWNER", "MANAGER", "HOSTESS").contains(role))) {
			try {
				StaffUserResponse grpcResponse = restaurantGrpcClient.getStaffForUser(
						StaffUserRequest.newBuilder().setUserId(user.getId().toString()).build()
				);

				if (!grpcResponse.getRestaurantId().isEmpty()) {
					// Since RestaurantStaff is a Record, use the constructor
					staffContext = new AuthDto.AuthResponse.RestaurantStaff(
							UUID.fromString(grpcResponse.getRestaurantId()),
							grpcResponse.getBranchId().isEmpty() ? null : UUID.fromString(grpcResponse.getBranchId()),
							grpcResponse.getEntityName(),
							grpcResponse.getActive()
					);
				}
			} catch (Exception e) {
				log.error("gRPC call failed to fetch management context: {}", e.getMessage());
			}
		}

		// 5. Use the Mapper (Updated to handle staff parameter)
		return authMapper.toAuthResponse(
				user,
				jwtTokenProvider.generateAccessToken(user),
				jwtTokenProvider.generateRefreshToken(user),
				jwtTokenProvider.getAccessTokenExpirySeconds(),
				staffContext // Pass the context into the mapper
		);
	}


	@Override
	@Transactional
	public void initiatePasswordReset(AuthDto.PasswordResetRequest request) {
		String email = request.email();

		if (!StringUtils.hasText(email)) {
			throw new InvalidRequestException("Email must be provided");
		}

		Optional<User> userOpt = userRepository.findByEmailAndDeletedFalse(email);
		User user = userOpt.orElseThrow(() -> new InvalidRequestException("User not found"));

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
	@Transactional
	public void resetPassword(AuthDto.PasswordResetConfirmRequest request) {
		String email = request.email();
		String newPassword = request.newPassword();
		String code = request.code();

		// Validate phone/email presence
		if (!StringUtils.hasText(email)) {
			throw new InvalidRequestException("Email must be provided");
		}
		if (!StringUtils.hasText(newPassword)) {
			throw new InvalidRequestException("Password must be provided");
		}
		if (!StringUtils.hasText(code)) {
			throw new InvalidRequestException("OTP code must be provided");
		}

		// Step 1: Verify OTP
		VerificationCode otpCode = verificationCodeRepository.findTopByTargetAndCodeHashAndUsedFalseOrderByCreatedAtDesc(email, code)
				.orElseThrow(() -> new InvalidRequestException("Invalid or expired OTP code"));

		// Step 2: Mark OTP as used
		otpCode.setUsed(true);
		verificationCodeRepository.save(otpCode);

		// Step 3: Find or create user by phone number
		User user = userRepository.findByEmailAndDeletedFalse(email)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		updatePasswordForUser(user.getId(), newPassword);
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
	}

	private void updatePasswordForUser(UUID userId, String newPassword) {
		UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(userId, AuthProvider.EMAIL_PASS)
				.orElseThrow(() -> new InvalidRequestException("Authentication method not found"));
		authMethod.setCredential(passwordEncoder.encode(newPassword));
		authMethod.setVerified(true);
		userAuthMethodRepository.save(authMethod);
	}

	// Helper stubs - implement these appropriately

	private boolean validateOAuthToken(String provider, String oauthToken) {
		// Validate with external OAuth provider
		return true;
	}

	private User findOrCreateUserByOAuth(AuthDto.OAuthLoginRequest request) {
		// Find or create user based on OAuth provider data
		return new User(); // stub
	}
}