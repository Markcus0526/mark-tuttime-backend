package com.tuttime.user.service.impl;

import com.auth0.jwk.UrlJwkProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.tuttime.common.enums.*;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.event.UserEvent;
import com.tuttime.common.event.proto.GetStaffForUserRequest;
import com.tuttime.common.event.proto.GetStaffForUserResponse;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.user.dto.AuthDto;
import com.tuttime.user.entity.*;
import com.tuttime.user.entity.enums.AuthProvider;
import com.tuttime.user.grpc.client.RestaurantGrpcClient;
import com.tuttime.user.mapper.AuthMapper;
import com.tuttime.user.provider.JwtTokenProvider;
import com.tuttime.user.provider.TokenProvider;
import com.tuttime.user.repository.*;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.TokenBlacklistService;
import com.tuttime.user.service.TokenWhitelistService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
	private final UserDeviceRepository userDeviceRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final AuthMapper authMapper;
	private final TokenProvider tokenProvider;
	private final RestaurantGrpcClient restaurantGrpcClient;
	private final EventProducer eventProducer;
	private final TokenBlacklistService tokenBlacklistService;
	private final TokenWhitelistService tokenWhitelistService;

	@Value("${app.oauth.google.client.id}")
	private String googleClientId;

	@Value("${app.oauth.apple.client.id}")
	private String appleClientId;

	// ================== REGISTRATION ==================
	@Override
	@Transactional
	public void registerUser(AuthDto.RegisterRequest request) {
		// ... (unchanged, keep your existing code) ...
		String phoneNumber = request.phoneNumber();
		String email = request.email();
		String password = request.password();

		if (!StringUtils.hasText(phoneNumber) && !StringUtils.hasText(email)) {
			throw new InvalidRequestException("Either phone number or email must be provided");
		}

		if (!StringUtils.hasText(phoneNumber) && StringUtils.hasText(email) && !StringUtils.hasText(password)) {
			throw new InvalidRequestException("Password must be provided");
		}

		if (StringUtils.hasText(request.phoneNumber()) &&
				userRepository.existsByPhoneNumberAndDeletedFalse(request.phoneNumber())) {
			throw new InvalidRequestException("Phone number in use");
		}

		if (StringUtils.hasText(request.email()) &&
				userRepository.existsByEmailAndDeletedFalse(request.email())) {
			throw new InvalidRequestException("Email in use");
		}

		User user = new User();
		user.setPhoneNumber(phoneNumber);
		user.setEmail(email);
		user.setStatus(UserStatus.PENDING_VERIFICATION);
		user.setLanguage(StringUtils.hasText(request.language()) ? request.language() : "en");
		user = userRepository.saveAndFlush(user);

		Role role = roleRepository.findByName(UserRoleKind.USER.name())
				.orElseThrow(() -> new InvalidRequestException("Role USER not found"));
		UserRole userRole = UserRole.builder().user(user).role(role).build();
		userRoleRepository.save(userRole);

		UserAuthMethod authMethod = new UserAuthMethod();
		authMethod.setUser(user);
		if (request.password() != null)
			authMethod.setCredential(passwordEncoder.encode(request.password()));
		else
			authMethod.setCredential(passwordEncoder.encode("1234567890"));
		authMethod.setAuthType(StringUtils.hasText(phoneNumber) ? AuthProvider.PHONE_SMS : AuthProvider.EMAIL_PASS);
		authMethod.setVerified(false);
		user.addAuthMethod(authMethod);
		userAuthMethodRepository.save(authMethod);
		userRepository.save(user);

		AuthDto.OtpVerificationRequest optRequest = new AuthDto.OtpVerificationRequest(phoneNumber, email);
		sendOtpVerificationCode(optRequest, NotificationTemplateKey.USER_REGISTER);
	}

	// ================== LOGIN ==================
	@Override
	@Transactional
	public void loginUser(AuthDto.LoginRequest request) {
		// ... (unchanged, keep your existing code) ...
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

		if (!user.getStatus().equals(UserStatus.ACTIVE)) {
			throw new ForbiddenException("User account is not active");
		}

		if (StringUtils.hasText(email)) {
			UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.EMAIL_PASS)
					.orElseThrow(() -> new ForbiddenException("Authentication method not found"));

			if (!passwordEncoder.matches(request.password(), authMethod.getCredential())) {
				throw new ForbiddenException("Invalid credentials");
			}

			if (!authMethod.isVerified()) {
				throw new ForbiddenException("User not verified");
			}
		}

		AuthDto.OtpVerificationRequest optRequest = new AuthDto.OtpVerificationRequest(phoneNumber, email);
		sendOtpVerificationCode(optRequest, NotificationTemplateKey.USER_LOGIN);
	}

	// ================== OAUTH LOGIN ==================
	@Override
	@Transactional
	public AuthDto.AuthResponse authenticateOAuthUser(AuthDto.OAuthLoginRequest request) {
		AuthDto.AuthResponse.OAuthUserInfo userInfo = validateOAuthToken(request.provider(), request.oauthToken());
		User user = findOrCreateUserByOAuth(request, userInfo);

		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);

		// Whitelist both tokens
		String accessJti = jwtTokenProvider.getJtiFromToken(accessToken);
		String refreshJti = jwtTokenProvider.getJtiFromToken(refreshToken);
		tokenWhitelistService.whitelist(accessJti, jwtTokenProvider.getAccessTokenExpirySeconds());
		tokenWhitelistService.whitelist(refreshJti, jwtTokenProvider.getRefreshTokenExpirySeconds());

		return authMapper.toAuthResponse(
				user,
				accessToken,
				refreshToken,
				jwtTokenProvider.getAccessTokenExpirySeconds(),
				null);
	}

	// ================== REFRESH TOKEN ==================
	@Override
	@Transactional
	public AuthDto.AuthResponse refreshJwtToken(String refreshToken) {
		if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
			throw new ForbiddenException("Invalid refresh token");
		}

		// Blacklist the old refresh token's JTI to prevent reuse
		Claims oldClaims = jwtTokenProvider.getRefreshClaims(refreshToken);
		String oldJti = oldClaims.getId();
		long remainingMs = oldClaims.getExpiration().getTime() - System.currentTimeMillis();
		if (remainingMs > 0) {
			tokenBlacklistService.blacklistJti(oldJti, remainingMs);
		}

		UUID userId = jwtTokenProvider.getUserIdFromRefreshToken(refreshToken);
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		String newAccessToken = jwtTokenProvider.generateAccessToken(user);
		String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);

		String accessJti = jwtTokenProvider.getJtiFromToken(newAccessToken);
		String refreshJti = jwtTokenProvider.getJtiFromToken(newRefreshToken);
		tokenWhitelistService.whitelist(accessJti, jwtTokenProvider.getAccessTokenExpirySeconds());
		tokenWhitelistService.whitelist(refreshJti, jwtTokenProvider.getRefreshTokenExpirySeconds());

		return authMapper.toAuthResponse(
				user,
				newAccessToken,
				newRefreshToken,
				jwtTokenProvider.getAccessTokenExpirySeconds(),
				null);
	}

	// ================== LOGOUT ==================
	@Override
	@Transactional
	public void logoutUser(String refreshToken, String fcmToken) {
		// Blacklist the refresh token JTI
		if (refreshToken != null && jwtTokenProvider.validateRefreshToken(refreshToken)) {
			Claims claims = jwtTokenProvider.getRefreshClaims(refreshToken);
			String jti = claims.getId();
			long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
			if (remainingMs > 0) {
				tokenBlacklistService.blacklistJti(jti, remainingMs);
			}
			tokenWhitelistService.remove(jti);
		}
		if (fcmToken != null) {
			userDeviceRepository.deleteByFcmToken(fcmToken);
		}
	}

	// ================== OTP VERIFICATION & LOGIN ==================
	@Override
	@Transactional
	public void sendOtpVerificationCode(AuthDto.OtpVerificationRequest request, NotificationTemplateKey templateKey) {
		// ... (unchanged, keep your existing code) ...
		String phoneNumber = request.phoneNumber();
		String email = request.email();

		if (!StringUtils.hasText(phoneNumber) && !StringUtils.hasText(email)) {
			throw new InvalidRequestException("Either phone number or email must be provided");
		}

		User user = (StringUtils.hasText(phoneNumber))
				? userRepository.findByPhoneNumberAndDeletedFalse(phoneNumber)
				  .orElseThrow(() -> new InvalidRequestException("Phone number not registered"))
				: userRepository.findByEmailAndDeletedFalse(email)
				  .orElseThrow(() -> new InvalidRequestException("Email not registered"));

		String code = tokenProvider.generateRandomCode();
		VerificationType type = StringUtils.hasText(phoneNumber) ? VerificationType.PHONE_VERIFY : VerificationType.EMAIL_VERIFY;

		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(type);
		verificationCode.setTarget(StringUtils.hasText(phoneNumber) ? phoneNumber : email);
		verificationCode.setCodeHash(passwordEncoder.encode(code));
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(600));
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);

		UserEvent.VerificationEvent event = UserEvent.VerificationEvent.builder()
				.userId(user.getId())
				.email(user.getEmail())
				.phoneNumber(user.getPhoneNumber())
				.verificationCode(code)
				.templateKey(templateKey)
				.type(type)
				.build();
		eventProducer.sendUserVerificationRequestedEvent(event);

		log.info("Verification event sent for user {} via {}", user.getId(), type);
	}

	@Override
	@Transactional
	public AuthDto.AuthResponse loginWithOtp(AuthDto.OtpVerificationConfirmRequest request) {
		String target = StringUtils.hasText(request.phoneNumber()) ? request.phoneNumber() : request.email();

		VerificationCode verificationCode = verificationCodeRepository
				.findTopByTargetAndUsedFalseOrderByCreatedAtDesc(target)
				.orElseThrow(() -> new InvalidRequestException("Invalid or expired OTP code"));

		if (!passwordEncoder.matches(request.code(), verificationCode.getCodeHash())) {
			throw new InvalidRequestException("Invalid or expired OTP code");
		}

		if (verificationCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
			throw new InvalidRequestException("Invalid or expired OTP code");
		}

		verificationCode.setUsed(true);
		verificationCodeRepository.save(verificationCode);

		User user = (verificationCode.getType() == VerificationType.PHONE_VERIFY)
				? userRepository.findByPhoneNumberAndDeletedFalse(target)
				  .orElseThrow(() -> new InvalidRequestException("User not found"))
				: userRepository.findByEmailAndDeletedFalse(target)
				  .orElseThrow(() -> new InvalidRequestException("User not found"));

		user.setStatus(UserStatus.ACTIVE);
		user.setLastLogin(OffsetDateTime.now());
		userRepository.save(user);

		if (verificationCode.getType() == VerificationType.EMAIL_VERIFY) {
			UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(user.getId(), AuthProvider.EMAIL_PASS)
					.orElseThrow(() -> new InvalidRequestException("Authentication method not found"));
			authMethod.setVerified(true);
			userAuthMethodRepository.save(authMethod);
		}

		// Fetch staff context (unchanged)
		AuthDto.AuthResponse.RestaurantStaffDto staffContext = null;
		List<String> roleNames = user.getRoles().stream()
				.map(ur -> ur.getRole().getName())
				.toList();

		if (roleNames.stream().anyMatch(role -> List.of("OWNER", "MANAGER", "HOSTESS").contains(role))) {
			try {
				GetStaffForUserResponse grpcResponse = restaurantGrpcClient.getStaffForUser(
						GetStaffForUserRequest.newBuilder().setUserId(user.getId().toString()).build()
				);
				if (!grpcResponse.getRestaurantId().isEmpty()) {
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

		// Generate tokens and whitelist them
		String accessToken = jwtTokenProvider.generateAccessToken(user);
		String refreshToken = jwtTokenProvider.generateRefreshToken(user);
		String accessJti = jwtTokenProvider.getJtiFromToken(accessToken);
		String refreshJti = jwtTokenProvider.getJtiFromToken(refreshToken);
		tokenWhitelistService.whitelist(accessJti, jwtTokenProvider.getAccessTokenExpirySeconds());
		tokenWhitelistService.whitelist(refreshJti, jwtTokenProvider.getRefreshTokenExpirySeconds());

		return authMapper.toAuthResponse(
				user,
				accessToken,
				refreshToken,
				jwtTokenProvider.getAccessTokenExpirySeconds(),
				staffContext
		);
	}

	// ================== PASSWORD RESET ==================
	@Override
	@Transactional
	public void initiatePasswordReset(AuthDto.PasswordResetRequest request) {
		// ... (unchanged, keep your existing code) ...
		String email = request.email();

		if (!StringUtils.hasText(email)) {
			throw new InvalidRequestException("Email must be provided");
		}

		User user = userRepository.findByEmailAndDeletedFalse(email)
				.orElseThrow(() -> new InvalidRequestException("User with this email not found"));

		String code = tokenProvider.generateRandomCode();

		VerificationCode verificationCode = new VerificationCode();
		verificationCode.setType(VerificationType.PASSWORD_RESET);
		verificationCode.setTarget(email);
		verificationCode.setCodeHash(passwordEncoder.encode(code));
		verificationCode.setExpiresAt(OffsetDateTime.now().plusSeconds(600));
		verificationCode.setUsed(false);
		verificationCodeRepository.save(verificationCode);

		UserEvent.VerificationEvent event = UserEvent.VerificationEvent.builder()
				.userId(user.getId())
				.email(email)
				.verificationCode(code)
				.type(VerificationType.EMAIL_VERIFY)
				.templateKey(NotificationTemplateKey.PASSWORD_RESET)
				.build();
		eventProducer.sendUserVerificationRequestedEvent(event);

		log.info("Password reset event sent to Kafka for user: {}", email);
	}

	@Override
	@Transactional
	public void resetPassword(AuthDto.PasswordResetConfirmRequest request) {
		// ... (unchanged, keep your existing code) ...
		String email = request.email();
		String newPassword = request.newPassword();
		String code = request.code();

		if (!StringUtils.hasText(email)) {
			throw new InvalidRequestException("Email must be provided");
		}
		if (!StringUtils.hasText(newPassword)) {
			throw new InvalidRequestException("Password must be provided");
		}
		if (!StringUtils.hasText(code)) {
			throw new InvalidRequestException("OTP code must be provided");
		}

		VerificationCode verificationCode = verificationCodeRepository
				.findTopByTargetAndUsedFalseOrderByCreatedAtDesc(email)
				.orElseThrow(() -> new InvalidRequestException("Invalid or expired OTP code"));

		if (!passwordEncoder.matches(request.code(), verificationCode.getCodeHash())) {
			throw new InvalidRequestException("Invalid or expired OTP code");
		}

		if (verificationCode.getExpiresAt().isBefore(OffsetDateTime.now())) {
			throw new InvalidRequestException("Invalid or expired OTP code");
		}

		verificationCode.setUsed(true);
		verificationCodeRepository.save(verificationCode);

		User user = userRepository.findByEmailAndDeletedFalse(email)
				.orElseThrow(() -> new InvalidRequestException("User not found"));

		updatePasswordForUser(user.getId(), newPassword);
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
	}

	// ================== DEVICE TOKEN ==================
	@Override
	@Transactional
	public void registerDevice(UUID userId, AuthDto.DeviceRegistrationRequest deviceInfo) {
		userDeviceRepository.upsertToken(
				userId,
				deviceInfo.token(),
				deviceInfo.platform().name(),
				deviceInfo.deviceName()
		);
	}

	// ================== USER BLOCK / UNBLOCK / DELETE (Admin) ==================
	@Override
	@Transactional
	public void blockUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));
		if (user.getStatus() == UserStatus.BLOCKED) {
			return;
		}
		user.setStatus(UserStatus.BLOCKED);
		userRepository.save(user);
		// Publish event to gateway
		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.type(UserStatusEvent.USER_BLOCKED)
				.userId(userId)
				.build();
		eventProducer.sendUserSecurityEvent(event);
		log.info("User {} blocked, event sent", userId);
	}

	@Override
	@Transactional
	public void unblockUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));
		if (user.getStatus() != UserStatus.BLOCKED) {
			return;
		}
		user.setStatus(UserStatus.ACTIVE);
		userRepository.save(user);
		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.type(UserStatusEvent.USER_UNBLOCKED)
				.userId(userId)
				.build();
		eventProducer.sendUserSecurityEvent(event);
		log.info("User {} unblocked, event sent", userId);
	}

	@Override
	@Transactional
	public void deleteUser(UUID userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new InvalidRequestException("User not found"));
		user.setDeleted(true);
		user.setDeletedAt(OffsetDateTime.now());
		userRepository.save(user);
		UserEvent.SecurityEvent event = UserEvent.SecurityEvent.builder()
				.type(UserStatusEvent.USER_DELETED)
				.userId(userId)
				.build();
		eventProducer.sendUserSecurityEvent(event);
		log.info("User {} deleted, event sent", userId);
	}

	// ================== PRIVATE HELPERS ==================
	private void updatePasswordForUser(UUID userId, String newPassword) {
		UserAuthMethod authMethod = userAuthMethodRepository.findByUserIdAndAuthType(userId, AuthProvider.EMAIL_PASS)
				.orElseThrow(() -> new InvalidRequestException("Authentication method not found"));
		authMethod.setCredential(passwordEncoder.encode(newPassword));
		authMethod.setVerified(true);
		userAuthMethodRepository.save(authMethod);
	}

	private AuthDto.AuthResponse.OAuthUserInfo validateOAuthToken(AuthProvider provider, String token) {
		return switch (provider) {
			case GOOGLE -> verifyGoogleToken(token);
			case APPLE -> verifyAppleToken(token);
			default -> throw new InvalidRequestException("Unsupported OAuth provider: " + provider);
		};
	}

	private AuthDto.AuthResponse.OAuthUserInfo verifyGoogleToken(String token) {
		try {
			GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
					.setAudience(Collections.singletonList(googleClientId))
					.setAcceptableTimeSkewSeconds(60)
					.build();
			GoogleIdToken idToken = verifier.verify(token);
			if (idToken == null) {
				throw new ForbiddenException("Invalid Google Token (Verification failed)");
			}
			GoogleIdToken.Payload payload = idToken.getPayload();
			return new AuthDto.AuthResponse.OAuthUserInfo(
					payload.getSubject(),
					payload.getEmail(),
					(String) payload.get("name"));
		} catch (Exception e) {
			throw new ForbiddenException("Google verification failed: " + e.getMessage());
		}
	}

	private AuthDto.AuthResponse.OAuthUserInfo verifyAppleToken(String token) {
		PublicKey applePublicKey = getApplePublicKey(token);
		Claims claims = Jwts.parser()
				.verifyWith(applePublicKey)
				.requireIssuer("https://appleid.apple.com")
				.requireAudience(appleClientId)
				.build()
				.parseSignedClaims(token)
				.getPayload();
		return new AuthDto.AuthResponse.OAuthUserInfo(
				claims.getSubject(),
				claims.get("email", String.class),
				null);
	}

	private User findOrCreateUserByOAuth(AuthDto.OAuthLoginRequest request, AuthDto.AuthResponse.OAuthUserInfo userInfo) {
		AuthProvider provider = request.provider();
		return userAuthMethodRepository.findByAuthTypeAndProviderId(provider, userInfo.providerId())
				.map(UserAuthMethod::getUser)
				.orElseGet(() -> {
					return userRepository.findByEmailAndDeletedFalse(userInfo.email())
							.map(existingUser -> {
								linkOAuthMethod(existingUser, provider, userInfo.providerId());
								return existingUser;
							})
							.orElseGet(() -> createNewOAuthUser(provider, userInfo));
				});
	}

	private User createNewOAuthUser(AuthProvider provider, AuthDto.AuthResponse.OAuthUserInfo userInfo) {
		User newUser = User.builder()
				.email(userInfo.email())
				.fullName(userInfo.fullName())
				.status(UserStatus.ACTIVE)
				.build();
		linkOAuthMethod(newUser, provider, userInfo.providerId());
		Role role = roleRepository.findByName(UserRoleKind.USER.name())
				.orElseThrow(() -> new InvalidRequestException("Role USER not found"));
		newUser.getRoles().add(UserRole.builder().user(newUser).role(role).build());
		return userRepository.save(newUser);
	}

	private void linkOAuthMethod(User user, AuthProvider provider, String oauthId) {
		UserAuthMethod method = UserAuthMethod.builder()
				.user(user)
				.authType(provider)
				.credential(oauthId)
				.verified(true)
				.build();
		user.addAuthMethod(method);
	}

	public PublicKey getApplePublicKey(String token) {
		try {
			var jwt = Jwts.parser().build().parse(token);
			String kid = ((io.jsonwebtoken.JwsHeader) jwt.getHeader()).getKeyId();
			UrlJwkProvider provider = new UrlJwkProvider(new URL("https://apple.com"));
			return provider.get(kid).getPublicKey();
		} catch (Exception e) {
			throw new ForbiddenException("Unable to retrieve Apple public key: " + e.getMessage());
		}
	}
}