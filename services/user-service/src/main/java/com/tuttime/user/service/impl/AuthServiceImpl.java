package com.tuttime.user.service.impl;

import com.auth0.jwk.UrlJwkProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.enums.UserRoleKind;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.UserEvent;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.common.event.proto.StaffUserRequest;
import com.tuttime.common.event.proto.StaffUserResponse;
import com.tuttime.user.dto.AuthDto;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.common.enums.UserStatus;
import com.tuttime.common.enums.VerificationType;
import com.tuttime.user.grpc.client.RestaurantGrpcClient;
import com.tuttime.user.mapper.AuthMapper;
import com.tuttime.user.repository.*;
import com.tuttime.user.security.JwtTokenProvider;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.util.TokenUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URL;
import java.security.PublicKey;
import java.time.OffsetDateTime;
import java.util.Collections;
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
	private final AuthMapper authMapper;
	private final TokenUtil tokenUtil;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final EventProducer eventProducer;

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
		sendOtpVerificationCode(optRequest, NotificationTemplateKey.USER_REGISTER);
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
		sendOtpVerificationCode(optRequest, NotificationTemplateKey.USER_LOGIN);
	}

	@Override
	public AuthDto.AuthResponse authenticateOAuthUser(AuthDto.OAuthLoginRequest request) {
		// 1. Validate and get user info from Provider
		AuthDto.AuthResponse.OAuthUserInfo userInfo = validateOAuthToken(request.provider(), request.oauthToken());

		// 2. Sync with local Database
		User user = findOrCreateUserByOAuth(request, userInfo);

		// 3. Generate internal tokens
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
	public void sendOtpVerificationCode(AuthDto.OtpVerificationRequest request, NotificationTemplateKey templateKey) {
		String phoneNumber = request.phoneNumber();
		String email = request.email();

		// 1. Validate phone/email presence
		if (!StringUtils.hasText(phoneNumber) && !StringUtils.hasText(email)) {
			throw new InvalidRequestException("Either phone number or email must be provided");
		}

		// 2. Fetch User and Validate Registration
		User user = (StringUtils.hasText(phoneNumber))
				? userRepository.findByPhoneNumberAndDeletedFalse(phoneNumber)
				.orElseThrow(() -> new InvalidRequestException("Phone number not registered"))
				: userRepository.findByEmailAndDeletedFalse(email)
				.orElseThrow(() -> new InvalidRequestException("Email not registered"));

		// 3. Generate and Save Verification Code
		String code = tokenUtil.generateRandomCode();
		VerificationType type = StringUtils.hasText(phoneNumber) ? VerificationType.PHONE_VERIFY : VerificationType.EMAIL_VERIFY;

		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(type);
		verificationCode.setTarget(StringUtils.hasText(phoneNumber) ? phoneNumber : email);
		verificationCode.setCodeHash(passwordEncoder.encode(code)); // Security: Hash the code in DB
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(600));
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);

		// 4. Send Event to Notification Service via Kafka
		// We send a single event; the Notification Service routes via 'type'
		UserEvent.VerificationEvent event = UserEvent.VerificationEvent.builder()
				.userId(user.getId())
				.email(user.getEmail())
				.phoneNumber(user.getPhoneNumber())
				.verificationCode(code) // Plain code sent ONLY to Kafka
				.templateKey(templateKey)
				.type(type)
				.build();

		eventProducer.sendUserVerificationRequestedEvent(event);

		log.info("Verification event sent for user {} via {}", user.getId(), type);
	}

	@Transactional
	public AuthDto.AuthResponse loginWithOtp(AuthDto.OtpVerificationConfirmRequest request) {
		String target = StringUtils.hasText(request.phoneNumber()) ? request.phoneNumber() : request.email();

		// 1. Find the latest unused code for the specific target (email or phone)
		VerificationCode verificationCode = verificationCodeRepository
				.findTopByTargetAndUsedFalseOrderByCreatedAtDesc(target)
				.orElseThrow(() -> new InvalidRequestException("Invalid or expired OTP code"));

		// 2. Security Check: Use .matches() to compare raw input with DB Hash
		// NEVER use passwordEncoder.encode() in a query; it generates a new salt every time.
		if (!passwordEncoder.matches(request.code(), verificationCode.getCodeHash())) {
			throw new InvalidRequestException("Invalid or expired OTP code");
		}

		// 3. Expiration Check: Compare against the expiresAt field you saved
		if (verificationCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
			throw new InvalidRequestException("Invalid or expired OTP code");
		}

		verificationCode.setUsed(true);
		verificationCodeRepository.save(verificationCode);

		// 2. Find User
		User user = (verificationCode.getType() == VerificationType.PHONE_VERIFY)
				? userRepository.findByPhoneNumberAndDeletedFalse(target)
				.orElseThrow(() -> new InvalidRequestException("User not found"))
				: userRepository.findByEmailAndDeletedFalse(target)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		// 3. Update User State
		user.setStatus(UserStatus.ACTIVE);
		user.setLastLogin(OffsetDateTime.now());
		userRepository.save(user);

		// Optional: If you want to ensure the user has an auth method for EMAIL_PASS
		if (verificationCode.getType() == VerificationType.EMAIL_VERIFY) {
			UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.EMAIL_PASS)
					.orElseThrow(() -> new InvalidRequestException("Authentication method not found"));
			authMethod.setVerified(true);
			userAuthMethodRepository.save(authMethod);
		}

		// 4. Fetch Management Context (gRPC) if applicable
		AuthDto.AuthResponse.RestaurantStaffDto staffContext = null;
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
					staffContext = new AuthDto.AuthResponse.RestaurantStaffDto(
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

		// 1. Fetch User
		User user = userRepository.findByEmailAndDeletedFalse(email)
				.orElseThrow(() -> new InvalidRequestException("User with this email not found"));

		// 2. Generate and Save Verification Code
		String code = tokenUtil.generateRandomCode();

		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(VerificationType.PASSWORD_RESET); // Use a specific type for password resets
		verificationCode.setTarget(email);
		verificationCode.setCodeHash(passwordEncoder.encode(code)); // Recommended: Hash the code
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(600)); // 10 min for reset is better
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);

		// 3. Send Event to Notification Service
		// We use a specific template key like 'PASSWORD_RESET' so the
		// Notification Service can use a different HTML template than OTP.
		UserEvent.VerificationEvent event = UserEvent.VerificationEvent.builder()
				.userId(user.getId())
				.email(email)
				.verificationCode(code)
				.type(VerificationType.EMAIL_VERIFY)
				.templateKey(NotificationTemplateKey.PASSWORD_RESET) // Explicitly set the template to use
				.build();

		eventProducer.sendUserVerificationRequestedEvent(event);

		log.info("Password reset event sent to Kafka for user: {}", email);
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

		// 1. Find the latest unused code for the specific target (email or phone)
		VerificationCode verificationCode = verificationCodeRepository
				.findTopByTargetAndUsedFalseOrderByCreatedAtDesc(email)
				.orElseThrow(() -> new InvalidRequestException("Invalid or expired OTP code"));

		// 2. Security Check: Use .matches() to compare raw input with DB Hash
		// NEVER use passwordEncoder.encode() in a query; it generates a new salt every time.
		if (!passwordEncoder.matches(request.code(), verificationCode.getCodeHash())) {
			throw new InvalidRequestException("Invalid or expired OTP code");
		}

		// 3. Expiration Check: Compare against the expiresAt field you saved
		if (verificationCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
			throw new InvalidRequestException("Invalid or expired OTP code");
		}

		// Step 2: Mark OTP as used
		verificationCode.setUsed(true);
		verificationCodeRepository.save(verificationCode);

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

	private AuthDto.AuthResponse.OAuthUserInfo validateOAuthToken(AuthProvider provider, String token) {
		return switch (provider) {
			case GOOGLE -> verifyGoogleToken(token);
			case APPLE -> verifyAppleToken(token);
			default -> throw new InvalidRequestException("Unsupported OAuth provider: " + provider);
		};
	}

	private AuthDto.AuthResponse.OAuthUserInfo verifyGoogleToken(String token) {
		GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
				.setAudience(Collections.singletonList("${app.oauth.google.client.id}")) // Ensure this matches your Client ID
				.build();

		try {
			GoogleIdToken idToken = verifier.verify(token);
			if (idToken == null) throw new ForbiddenException("Invalid Google Token");

			GoogleIdToken.Payload payload = idToken.getPayload();
			return new AuthDto.AuthResponse.OAuthUserInfo(
					payload.getSubject(),    // providerId
					payload.getEmail(),      // email
					(String) payload.get("name")); // name
		} catch (Exception e) {
			throw new ForbiddenException("Google verification failed");
		}
	}

	private AuthDto.AuthResponse.OAuthUserInfo verifyAppleToken(String token) {
		PublicKey applePublicKey = getApplePublicKey(token);
		Claims claims = Jwts.parser()
				.verifyWith(applePublicKey)
				.requireIssuer("https://appleid.apple.com")
				.requireAudience("${app.oauth.apple.client.id}")
				.build()
				.parseSignedClaims(token)
				.getPayload();

		return new AuthDto.AuthResponse.OAuthUserInfo(
				claims.getSubject(),     // providerId
				claims.get("email", String.class), // email
				null); // Apple often doesn't send the name in the token
	}

	private User findOrCreateUserByOAuth(AuthDto.OAuthLoginRequest request, AuthDto.AuthResponse.OAuthUserInfo userInfo) {
		AuthProvider provider = request.provider();

		// 1. Check if this specific OAuth account is already linked
		return userAuthMethodRepository.findByAuthTypeAndProviderId(provider, userInfo.providerId())
				.map(UserAuthMethod::getUser)
				.orElseGet(() -> {
					// 2. If not, check if a user with this email already exists
					return userRepository.findByEmailAndDeletedFalse(userInfo.email())
							.map(existingUser -> {
								// Link new OAuth method to existing account
								linkOAuthMethod(existingUser, request.provider(), userInfo.providerId());
								return existingUser;
							})
							.orElseGet(() -> {
								// 3. Create a brand new User
								return createNewOAuthUser(request.provider(), userInfo);
							});
				});
	}

	private User createNewOAuthUser(AuthProvider provider, AuthDto.AuthResponse.OAuthUserInfo userInfo) {
		User newUser = User.builder()
				.email(userInfo.email())
				.fullName(userInfo.fullName())
				.status(UserStatus.ACTIVE) // OAuth users are usually pre-verified
				.build();

		linkOAuthMethod(newUser, provider, userInfo.providerId());

		// Add default role
		// UserRole role = UserRole.builder().user(newUser).role(Role.USER).build();
		// newUser.getRoles().add(role);

		return userRepository.save(newUser);
	}

	private void linkOAuthMethod(User user, AuthProvider provider, String oauthId) {
		UserAuthMethod method = UserAuthMethod.builder()
				.user(user)
				.authType(provider)
				.credential(oauthId) // Store the 'sub' ID here
				.verified(true)      // OAuth providers are pre-verified
				.build();
		user.addAuthMethod(method);
	}

	public PublicKey getApplePublicKey(String token) {
		try {
			// 1. Parse the token without verification to extract 'kid' from header
//			String kid = Jwts.parser()
//					.build()
//					.parse(token)
////					.accept(Jws.class)
//					.getHeader()
//					.getKeyId();

			var jwt = Jwts.parser().build().parse(token);
			String kid = ((io.jsonwebtoken.JwsHeader) jwt.getHeader()).getKeyId();

			// 2. Use the Auth0 library to fetch Apple's keys and find the match
			UrlJwkProvider provider = new UrlJwkProvider(new URL("https://apple.com"));
			return provider.get(kid).getPublicKey();
		} catch (Exception e) {
			throw new ForbiddenException("Unable to retrieve Apple public key: " + e.getMessage());
		}
	}
}