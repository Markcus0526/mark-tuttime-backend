package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.CommonMessageKeys;
import com.tuttime.common.kafka.EventProducer;
import com.tuttime.common.kafka.ReferralEvent;
import com.tuttime.user.exception.UserServiceMessageKeys;
import com.tuttime.user.dto.LoginRequest;
import com.tuttime.user.dto.LoginResponse;
import com.tuttime.user.dto.UserRegistrationRequest;
import com.tuttime.user.dto.UserResponse;
import com.tuttime.user.service.SmsVerificationService;
import com.tuttime.user.entity.User;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.util.JwtTokenUtil;
import com.tuttime.user.util.PhoneNumberUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/** Authentication Service: handles user authentication, JWT token generation, whitelisting/blacklisting, and refresh token rotation. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenWhitelistService tokenWhitelistService;
    private final BruteForceProtectionService bruteForceProtectionService;
    private final AntiFraudService antiFraudService;
    private final SmsVerificationService smsVerificationService;
    private final com.tuttime.user.grpc.client.RewardsServiceGrpcClient rewardsServiceGrpcClient;
    private final com.tuttime.user.rest.client.RewardsServiceRestClient rewardsServiceRestClient;
    private final ImageUrlCacheService imageUrlCacheService;
    private final ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    @Autowired(required = false)
    private EventProducer eventProducer;

    @Value("${spring.security.jwt.expiration}")
    private Long jwtExpiration;

    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    private static final String REFRESH_RESPONSE_CACHE_PREFIX = "refresh:response:";

    /** Completes user signup in a single transaction: validates token/device, registers user, invalidates token, generates tokens. */
    @Transactional(rollbackFor = Exception.class)
    public LoginResponse completeSignup(
            UserRegistrationRequest registrationRequest,
            String signupToken,
            HttpServletRequest httpRequest) {
        log.info("Completing signup for phone: {}", registrationRequest.getPhone());
        
        boolean tokenValidated = false;
        String verifiedPhoneNumber = null;

        try {
            // Validate signup token and extract phone number (already normalized)
            verifiedPhoneNumber = smsVerificationService.validateSignupToken(signupToken);
            tokenValidated = true;

            // Normalize phone number from request for comparison
            String normalizedRequestPhone = PhoneNumberUtil.normalizePhoneNumber(registrationRequest.getPhone());

            // Verify phone number matches (after normalization)
            if (!verifiedPhoneNumber.equals(normalizedRequestPhone)) {
                log.warn("Phone number mismatch in signup: verifiedPhone={}, requestPhone={}, normalizedRequestPhone={}", 
                        verifiedPhoneNumber, registrationRequest.getPhone(), normalizedRequestPhone);
                throw new BusinessException(
                        "Phone number does not match the signup token",
                        UserServiceMessageKeys.PHONE_NUMBER_DOES_NOT_MATCH_SIGNUP_TOKEN);
            }
            
            // Update registration request with normalized phone number for consistent storage
            registrationRequest = UserRegistrationRequest.builder()
                    .phone(verifiedPhoneNumber) // Use normalized phone from token
                    .password(registrationRequest.getPassword())
                    .email(registrationRequest.getEmail())
                    .username(registrationRequest.getUsername())
                    .firstName(registrationRequest.getFirstName())
                    .lastName(registrationRequest.getLastName())
                    .telegram(registrationRequest.getTelegram())
                    .instagram(registrationRequest.getInstagram())
                    .country(registrationRequest.getCountry())
                    .city(registrationRequest.getCity())
                    .dateOfBirth(registrationRequest.getDateOfBirth())
                    .gender(registrationRequest.getGender())
                    .referralCode(registrationRequest.getReferralCode())
                    .interests(registrationRequest.getInterests())
                    .latitude(registrationRequest.getLatitude())
                    .longitude(registrationRequest.getLongitude())
                    .locationAccuracy(registrationRequest.getLocationAccuracy())
                    .build();

            // Extract device ID and IP address
            String clientDeviceId = extractDeviceIdFromRequest(httpRequest);
            String ip = extractIpAddress(httpRequest);

            // Log device information for signup
            log.info("Signup device information: phone={}, email={}, clientDeviceId={}, ip={}, " +
                    "userAgent={}, deviceType={}, deviceModel={}, osVersion={}, appVersion={}",
                    registrationRequest.getPhone(),
                    registrationRequest.getEmail(),
                    clientDeviceId != null ? clientDeviceId : "null",
                    ip != null ? ip : "null",
                    httpRequest != null ? httpRequest.getHeader("User-Agent") : "null",
                    httpRequest != null ? httpRequest.getHeader("X-Device-Type") : "null",
                    httpRequest != null ? httpRequest.getHeader("X-Device-Model") : "null",
                    httpRequest != null ? httpRequest.getHeader("X-OS-Version") : "null",
                    httpRequest != null ? httpRequest.getHeader("X-App-Version") : "null");

            // Validate device registration (anti-fraud) - device ID is required
            try {
                antiFraudService.validateDeviceRegistration(clientDeviceId, ip, 0L);
                log.info("Device validation passed for signup: clientDeviceId={}, ip={}",
                        clientDeviceId, ip);
            } catch (BusinessException e) {
                log.warn("Device registration validation failed during signup: clientDeviceId={}, ip={}, error={}", 
                        clientDeviceId, ip, e.getMessage());
                throw e;
            }

            // Register user (this will create user and device records)
            UserResponse userResponse = userService.registerUser(registrationRequest);

            // Handle referral code if provided
            // Note: Referral processing is non-critical - failures should not rollback the signup transaction
            if (registrationRequest.getReferralCode() != null && !registrationRequest.getReferralCode().trim().isEmpty()) {
                try {
                    String referralCode = registrationRequest.getReferralCode().trim().toUpperCase();
                    log.info("Publishing referral code event during signup: referralCode={}, newUserId={}", 
                            referralCode, userResponse.getUserId());
                    
                    // Publish referral event to Kafka for async processing by Rewards Service
                    // This is non-blocking and non-critical - signup should succeed even if Kafka is unavailable
                    if (eventProducer != null) {
                        ReferralEvent referralEvent = ReferralEvent.builder()
                                .eventType(ReferralEvent.ReferralEventType.REFERRAL_CODE_SUBMITTED)
                                .refereeId(userResponse.getUserId())
                                .referralCode(referralCode)
                                .build();
                        
                        eventProducer.publishReferralEvent(referralEvent);
                        log.debug("Published referral event: referralCode={}, refereeId={}", 
                                referralCode, userResponse.getUserId());
                    } else {
                        log.warn("Kafka EventProducer not available - referral code will not be processed: referralCode={}, userId={}", 
                                referralCode, userResponse.getUserId());
                    }
                } catch (Exception e) {
                    // Kafka publishing failures are non-critical - don't fail signup
                    log.error("Error publishing referral event during signup (non-critical, continuing signup): referralCode={}, userId={}, error={}", 
                            registrationRequest.getReferralCode(), userResponse.getUserId(), e.getMessage(), e);
                    // Don't fail signup if referral event publishing fails - referral is non-critical
                }
            }

            // Invalidate signup token only after successful signup
            smsVerificationService.invalidateSignupToken(signupToken);
            log.debug("Signup token invalidated after successful signup: phone={}", verifiedPhoneNumber);

            // Reset brute-force attempts before automatic login after signup
            // This prevents blocking legitimate signup flow due to previous failed login attempts
            String phoneNumber = registrationRequest.getPhone();
            if (ip != null && phoneNumber != null) {
                bruteForceProtectionService.resetAttempts(ip, phoneNumber);
                log.debug("Reset brute-force attempts for signup: IP={}, phone={}", ip, phoneNumber);
            }

            // Generate tokens for immediate login
            LoginRequest loginRequest = LoginRequest.builder()
                    .email(userResponse.getEmail())
                    .password(registrationRequest.getPassword())
                    .build();

            LoginResponse loginResponse = login(loginRequest);

            return loginResponse;
        } catch (Exception e) {
            // If signup fails after token validation, log the error but don't invalidate token
            // This allows user to retry signup with the same token if validation failed
            // However, if it's a business rule violation (like username taken), token should be invalidated
            if (tokenValidated && e instanceof BusinessException) {
                BusinessException be = (BusinessException) e;
                // Invalidate token for certain errors that indicate the signup cannot be retried
                // (e.g., username already taken, phone already registered)
                String messageKey = be.getMessageKey();
                if (messageKey != null && (
                    messageKey.contains("PHONE_NUMBER_ALREADY_REGISTERED")
                )) {
                    log.warn("Signup failed with non-retryable error, invalidating token: messageKey={}, phone={}", 
                            messageKey, verifiedPhoneNumber);
                    smsVerificationService.invalidateSignupToken(signupToken);
                }
            }
            throw e;
        }
    }

    /**
     * Authenticates a user and generates JWT tokens.
     *
     * <p>This method:
     * <ol>
     *   <li>Validates email and password</li>
     *   <li>Checks account status (active, not blocked)</li>
     *   <li>Updates last login timestamp</li>
     *   <li>Generates access token and refresh token</li>
     * </ol>
     *
     * <p><b>Business Rules:</b>
     * <ul>
     *   <li>Email must exist in the system</li>
     *   <li>Password must match the stored hash</li>
     *   <li>Account must be active and not blocked</li>
     * </ul>
     *
     * @param request The login request containing email and password
     * @return LoginResponse containing JWT tokens and user information
     * @throws BusinessException if credentials are invalid or account is inactive/blocked
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());

        // Extract IP address and phone number for brute-force protection
        String ip = extractIpAddress();
        String phoneNumber = null; // Will be set after finding user

        // Find user first to get phone number
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    // Record failed attempt even if user doesn't exist (to prevent enumeration)
                    if (ip != null) {
                        bruteForceProtectionService.recordFailedAttempt(ip, request.getEmail());
                    }
                    return new BusinessException(
                            "Invalid email or password",
                            UserServiceMessageKeys.INVALID_EMAIL_OR_PASSWORD);
                });

        // Set phone number from user
        phoneNumber = user.getPhone();

        // Check brute-force protection before password validation
        if (bruteForceProtectionService.isBlocked(ip, phoneNumber)) {
            long remainingTime = bruteForceProtectionService.getRemainingBlockTime(ip, phoneNumber);
            log.warn("Login blocked due to brute-force protection: IP={}, phone={}, remainingTime={}s", 
                    ip, phoneNumber, remainingTime);
            throw new BusinessException(
                    String.format("Too many failed login attempts. Please try again in %d seconds.", remainingTime),
                    UserServiceMessageKeys.TOO_MANY_FAILED_ATTEMPTS);
        }

        // Validate password
        boolean passwordValid = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        
        if (!passwordValid) {
            // Record failed attempt
            bruteForceProtectionService.recordFailedAttempt(ip, phoneNumber);
            throw new BusinessException(
                    "Invalid email or password",
                    UserServiceMessageKeys.INVALID_EMAIL_OR_PASSWORD);
        }

        if (!user.getIsActive()) {
            throw new BusinessException(
                    "User account is inactive",
                    UserServiceMessageKeys.USER_ACCOUNT_INACTIVE);
        }

        if (user.getIsPaused() != null && user.getIsPaused()) {
            throw new BusinessException(
                    "User account is paused",
                    UserServiceMessageKeys.USER_ACCOUNT_INACTIVE);
        }

        if (user.getIsBlocked()) {
            throw new BusinessException(
                    "User account is blocked",
                    UserServiceMessageKeys.USER_ACCOUNT_BLOCKED);
        }

        // Reset failed attempts on successful login
        bruteForceProtectionService.resetAttempts(ip, phoneNumber);

        // Extract device ID from request
        String deviceId = extractDeviceIdFromRequest();

        // Track device usage for anti-fraud AFTER transaction commits
        // This prevents lock contention and timeouts when using REQUIRES_NEW propagation
        // Device tracking is non-critical and should not block login
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            final String finalDeviceId = deviceId;
            final Long finalUserId = user.getUserId();
            final String finalIp = ip;
            
            // Execute device tracking after transaction commits to avoid lock contention
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                log.debug("Transaction committed, tracking device usage: deviceId={}, userId={}", 
                                        finalDeviceId, finalUserId);
                                // Explicitly set datasource context to "write" for the new transaction
                                // This ensures proper routing when trackDeviceUsage creates a new transaction
                                com.tuttime.common.config.ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.setDataSourceType("write");
                                try {
                                    antiFraudService.trackDeviceUsage(finalDeviceId, finalUserId, finalIp);
                                    log.debug("Device usage tracked successfully: deviceId={}, userId={}", 
                                            finalDeviceId, finalUserId);
                                } finally {
                                    // Clear datasource context after trackDeviceUsage completes
                                    com.tuttime.common.config.ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.clearDataSourceType();
                                }
                            } catch (Exception e) {
                                // Log but don't fail - device tracking is non-critical
                                log.error("Failed to track device usage after transaction commit (non-critical): deviceId={}, userId={}, error={}", 
                                        finalDeviceId, finalUserId, e.getMessage(), e);
                                // Don't fail login if device tracking fails - this runs after commit
                            }
                        }
                    }
            );
        }

        // Update last login - wrap in try-catch to prevent exceptions from marking transaction for rollback
        // Last login update is non-critical - login should succeed even if this fails
        try {
            userService.updateLastLogin(user.getUserId());
        } catch (org.springframework.dao.DataAccessException e) {
            // Database exceptions during last login update - log but don't fail login
            log.error("DataAccessException updating last login (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
            // Don't rethrow - last login update is non-critical
        } catch (Exception e) {
            // Other exceptions during last login update - log but don't fail login
            log.error("Exception updating last login (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
            // Don't rethrow - last login update is non-critical
        }

        // Generate JTI for access token and refresh token
        String accessTokenJti = jwtTokenUtil.generateJti();
        String refreshTokenJti = jwtTokenUtil.generateJti();

        // Generate JWT tokens with pre-generated JTIs and device ID
        String token = jwtTokenUtil.generateToken(
                user.getUserId(),
                user.getEmail(),
                accessTokenJti,
                deviceId
        );
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUserId(), refreshTokenJti, deviceId);

        // Get token expiration times
        Long accessTokenExpiration = jwtTokenUtil.getExpirationDateFromToken(token).getTime();
        Long refreshTokenExpiration = jwtTokenUtil.getExpirationDateFromToken(refreshToken).getTime();

        // Add tokens to whitelist with device ID - wrap in try-catch to prevent exceptions from marking transaction for rollback
        // Whitelisting failures should not rollback the login transaction
        boolean accessTokenWhitelisted = false;
        boolean refreshTokenWhitelisted = false;
        
        try {
            accessTokenWhitelisted = tokenWhitelistService.addToWhitelist(
                    accessTokenJti,
                    user.getUserId(),
                    "access",
                    accessTokenExpiration,
                    deviceId
            );
            refreshTokenWhitelisted = tokenWhitelistService.addToWhitelist(
                    refreshTokenJti,
                    user.getUserId(),
                    "refresh",
                    refreshTokenExpiration,
                    deviceId
            );
        } catch (org.springframework.dao.DataAccessException e) {
            // Database/Redis exceptions during whitelisting - log but don't fail login
            log.error("DataAccessException during token whitelisting (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
        } catch (org.springframework.transaction.TransactionException e) {
            // Transaction exceptions during whitelisting - log but don't fail login
            log.error("TransactionException during token whitelisting (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
        } catch (Exception e) {
            // Other exceptions during whitelisting - log but don't fail login
            log.error("Exception during token whitelisting (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
        }

        // Log whitelisting status - don't throw exceptions that would rollback transaction
        if (redisTemplate != null && (!accessTokenWhitelisted || !refreshTokenWhitelisted)) {
            log.error("Failed to whitelist tokens for user: {}, accessTokenWhitelisted={}, refreshTokenWhitelisted={}. " +
                    "Login will proceed but tokens may be rejected by gateway.", 
                    user.getUserId(), accessTokenWhitelisted, refreshTokenWhitelisted);
        }

        log.debug("Generated and whitelisted JWT tokens for user: {}, accessJti={}, refreshJti={}", 
                user.getUserId(), accessTokenJti, refreshTokenJti);

        UserResponse userResponse = userMapper.toResponse(user);
        // Enrich with image URL (for backward compatibility with legacy keys)
        userMapper.enrichImageUrl(userResponse, imageUrlCacheService);

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userResponse)
                .expiresIn(jwtExpiration)
                .build();
    }

    /**
     * Authenticates a user with phone number and password.
     *
     * <p>This method is similar to {@link #login(LoginRequest)} but uses phone number
     * instead of email for authentication.
     *
     * @param phoneNumber Phone number
     * @param password Password
     * @return LoginResponse containing JWT tokens and user information
     * @throws BusinessException if credentials are invalid or account is inactive/blocked
     */
    @Transactional
    public LoginResponse signinWithPhone(String phoneNumber, String password) {
        log.info("Phone-based signin attempt for phone: {}", phoneNumber);

        // Extract IP address for brute-force protection
        String ip = extractIpAddress();

        // Normalize phone number before lookup and brute-force checks
        String normalizedPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);
        
        // Check brute-force protection before proceeding
        if (bruteForceProtectionService.isBlocked(ip, normalizedPhone)) {
            long remainingTime = bruteForceProtectionService.getRemainingBlockTime(ip, normalizedPhone);
            log.warn("Signin blocked due to brute-force protection: IP={}, phone={}, remainingTime={}s", 
                    ip, normalizedPhone, remainingTime);
            throw new BusinessException(
                    String.format("Too many failed login attempts. Please try again in %d seconds.", remainingTime),
                    UserServiceMessageKeys.TOO_MANY_FAILED_ATTEMPTS);
        }
        
        // Find user by phone number
        User user = userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> {
                    // Record failed attempt even if user doesn't exist (to prevent enumeration)
                    bruteForceProtectionService.recordFailedAttempt(ip, normalizedPhone);
                    return new BusinessException(
                            "Invalid phone number or password",
                            UserServiceMessageKeys.INVALID_PHONE_NUMBER_OR_PASSWORD);
                });

        // Validate password
        boolean passwordValid = passwordEncoder.matches(password, user.getPasswordHash());
        
        if (!passwordValid) {
            // Record failed attempt
            bruteForceProtectionService.recordFailedAttempt(ip, normalizedPhone);
            throw new BusinessException(
                    "Invalid phone number or password",
                    UserServiceMessageKeys.INVALID_PHONE_NUMBER_OR_PASSWORD);
        }

        if (!user.getIsActive()) {
            bruteForceProtectionService.recordFailedAttempt(ip, normalizedPhone);
            throw new BusinessException(
                    "User account is inactive",
                    UserServiceMessageKeys.USER_ACCOUNT_INACTIVE);
        }

        if (user.getIsBlocked()) {
            bruteForceProtectionService.recordFailedAttempt(ip, normalizedPhone);
            throw new BusinessException(
                    "User account is blocked",
                    UserServiceMessageKeys.USER_ACCOUNT_BLOCKED);
        }

        // Reset failed attempts on successful login
        bruteForceProtectionService.resetAttempts(ip, normalizedPhone);

        // Extract device ID from request
        String deviceId = extractDeviceIdFromRequest();

        // Track device usage for anti-fraud AFTER transaction commits
        // This prevents lock contention and timeouts when using REQUIRES_NEW propagation
        // Device tracking is non-critical and should not block login
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            final String finalDeviceId = deviceId;
            final Long finalUserId = user.getUserId();
            final String finalIp = ip;
            
            // Execute device tracking after transaction commits to avoid lock contention
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                log.debug("Transaction committed, tracking device usage: deviceId={}, userId={}", 
                                        finalDeviceId, finalUserId);
                                // Explicitly set datasource context to "write" for the new transaction
                                // This ensures proper routing when trackDeviceUsage creates a new transaction
                                com.tuttime.common.config.ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.setDataSourceType("write");
                                try {
                                    antiFraudService.trackDeviceUsage(finalDeviceId, finalUserId, finalIp);
                                    log.debug("Device usage tracked successfully: deviceId={}, userId={}", 
                                            finalDeviceId, finalUserId);
                                } finally {
                                    // Clear datasource context after trackDeviceUsage completes
                                    com.tuttime.common.config.ReadWriteDataSourceConfig.ReadWriteRoutingDataSourceContext.clearDataSourceType();
                                }
                            } catch (Exception e) {
                                // Log but don't fail - device tracking is non-critical
                                log.error("Failed to track device usage after transaction commit (non-critical): deviceId={}, userId={}, error={}", 
                                        finalDeviceId, finalUserId, e.getMessage(), e);
                                // Don't fail login if device tracking fails - this runs after commit
                            }
                        }
                    }
            );
        }

        // Update last login - wrap in try-catch to prevent exceptions from marking transaction for rollback
        // Last login update is non-critical - login should succeed even if this fails
        try {
            userService.updateLastLogin(user.getUserId());
        } catch (org.springframework.dao.DataAccessException e) {
            // Database exceptions during last login update - log but don't fail login
            log.error("DataAccessException updating last login (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
            // Don't rethrow - last login update is non-critical
        } catch (Exception e) {
            // Other exceptions during last login update - log but don't fail login
            log.error("Exception updating last login (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
            // Don't rethrow - last login update is non-critical
        }

        // Generate JTI for access token and refresh token
        String accessTokenJti = jwtTokenUtil.generateJti();
        String refreshTokenJti = jwtTokenUtil.generateJti();

        // Generate JWT tokens with pre-generated JTIs and device ID
        String token = jwtTokenUtil.generateToken(
                user.getUserId(),
                user.getEmail(),
                accessTokenJti,
                deviceId
        );
        String refreshToken = jwtTokenUtil.generateRefreshToken(user.getUserId(), refreshTokenJti, deviceId);

        // Get token expiration times
        Long accessTokenExpiration = jwtTokenUtil.getExpirationDateFromToken(token).getTime();
        Long refreshTokenExpiration = jwtTokenUtil.getExpirationDateFromToken(refreshToken).getTime();

        // Add tokens to whitelist with device ID - wrap in try-catch to prevent exceptions from marking transaction for rollback
        // Whitelisting failures should not rollback the login transaction
        boolean accessTokenWhitelisted = false;
        boolean refreshTokenWhitelisted = false;
        
        try {
            accessTokenWhitelisted = tokenWhitelistService.addToWhitelist(
                    accessTokenJti,
                    user.getUserId(),
                    "access",
                    accessTokenExpiration,
                    deviceId
            );
            refreshTokenWhitelisted = tokenWhitelistService.addToWhitelist(
                    refreshTokenJti,
                    user.getUserId(),
                    "refresh",
                    refreshTokenExpiration,
                    deviceId
            );
        } catch (org.springframework.dao.DataAccessException e) {
            // Database/Redis exceptions during whitelisting - log but don't fail login
            // These exceptions shouldn't occur (Redis doesn't participate in DB transactions),
            // but if they do, we don't want to rollback the login transaction
            log.error("DataAccessException during token whitelisting (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
        } catch (org.springframework.transaction.TransactionException e) {
            // Transaction exceptions during whitelisting - log but don't fail login
            log.error("TransactionException during token whitelisting (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
        } catch (Exception e) {
            // Other exceptions during whitelisting - log but don't fail login
            log.error("Exception during token whitelisting (non-critical, continuing login): userId={}, error={}", 
                    user.getUserId(), e.getMessage(), e);
        }

        // Verify whitelisting succeeded - if Redis is available, whitelisting should succeed
        // However, if it fails, we log a warning but don't throw an exception that would rollback the transaction
        if (redisTemplate != null) {
            if (!accessTokenWhitelisted) {
                log.error("Failed to whitelist access token for user: {}, jti={}. Login will proceed but token may be rejected by gateway.", 
                        user.getUserId(), accessTokenJti);
                // Don't throw exception - whitelisting failure shouldn't rollback login transaction
            }
            if (!refreshTokenWhitelisted) {
                log.error("Failed to whitelist refresh token for user: {}, jti={}. Login will proceed but token may be rejected by gateway.", 
                        user.getUserId(), refreshTokenJti);
                // Don't throw exception - whitelisting failure shouldn't rollback login transaction
            }
            // Verify tokens are actually in whitelist (defense in depth)
            // Wrap in try-catch to prevent exceptions from marking transaction for rollback
            try {
                if (!tokenWhitelistService.isTokenWhitelisted(accessTokenJti)) {
                    log.error("Access token whitelist verification failed for user: {}, jti={}. Login will proceed but token may be rejected by gateway.", 
                            user.getUserId(), accessTokenJti);
                    // Don't throw exception - verification failure shouldn't rollback login transaction
                }
            } catch (Exception e) {
                log.error("Exception during token whitelist verification (non-critical, continuing login): userId={}, error={}", 
                        user.getUserId(), e.getMessage(), e);
                // Don't rethrow - verification is non-critical
            }
        } else {
            log.warn("Redis not available - tokens generated but not whitelisted. Token validation may fail in gateway.");
        }

        log.debug("Generated and whitelisted JWT tokens for user: {}, accessJti={}, refreshJti={}",
                user.getUserId(), accessTokenJti, refreshTokenJti);

        UserResponse userResponse = userMapper.toResponse(user);
        // Enrich with image URL (for backward compatibility with legacy keys)
        userMapper.enrichImageUrl(userResponse, imageUrlCacheService);

        return LoginResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .user(userResponse)
                .expiresIn(jwtExpiration)
                .build();
    }

    /**
     * Logout a user by removing token from whitelist and adding to blacklist
     *
     * <p>This method invalidates the JWT token by:
     * <ol>
     *   <li>Removing it from the whitelist (so it can't be used)</li>
     *   <li>Adding it to the blacklist (for additional security)</li>
     * </ol>
     * The token will remain blacklisted until it naturally expires.
     *
     * @param token The JWT token to invalidate
     * @param userId The user ID associated with the token
     */
    @Transactional
    public void logout(String token, Long userId) {
        log.info("Logout request for user: {}", userId);

        // Validate token belongs to user
        if (!jwtTokenUtil.validateToken(token, userId)) {
            throw new BusinessException(
                    "Invalid token",
                    CommonMessageKeys.INVALID_TOKEN);
        }

        // Extract JTI from token
        String jti = jwtTokenUtil.getJtiFromToken(token);
        if (jti != null) {
            // Remove token from whitelist
            tokenWhitelistService.removeFromWhitelist(jti);
            log.debug("Token removed from whitelist: jti={}, userId={}", jti, userId);
        } else {
            log.warn("Token missing JTI claim, cannot remove from whitelist: userId={}", userId);
        }

        // Get token expiration time
        Long expirationTime = jwtTokenUtil.getExpirationDateFromToken(token).getTime();
        Long currentTime = System.currentTimeMillis();
        Long ttl = expirationTime - currentTime;

        // Add token to blacklist (for additional security)
        if (ttl > 0 && redisTemplate != null) {
            // Add token to blacklist with TTL matching token expiration + buffer for clock skew (add 60s buffer)
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            long ttlWithBuffer = ttl + 60000; // Add 60s buffer to prevent premature expiration due to clock skew
            try {
                redisTemplate.opsForValue().set(blacklistKey, "logout", ttlWithBuffer, TimeUnit.MILLISECONDS);
                log.debug("Token blacklisted for user: {}, TTL: {} ms (with buffer)", userId, ttlWithBuffer);
            } catch (Exception e) {
                log.warn("Failed to blacklist token in Redis: {}. Token will remain valid until expiration.", e.getMessage());
            }
        } else if (ttl > 0 && redisTemplate == null) {
            log.warn("Redis not available - token blacklisting skipped. Token will remain valid until expiration.");
        } else {
            log.debug("Token already expired, no need to blacklist");
        }
    }

    /**
     * Check if a token is blacklisted
     *
     * @param token The JWT token to check
     * @return true if token is blacklisted (always false if Redis is not available)
     */
    public boolean isTokenBlacklisted(String token) {
        if (redisTemplate == null) {
            // Redis not available - cannot check blacklist (stateless mode)
            return false;
        }
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.warn("Failed to check token blacklist in Redis: {}. Assuming token is not blacklisted.", e.getMessage());
            return false; // Fail open - if Redis check fails, assume token is valid to prevent false rejections
        }
    }

    /**
     * Extract user ID from token
     *
     * @param token The JWT token
     * @return User ID or null if token is invalid
     */
    public Long getUserIdFromToken(String token) {
        try {
            return jwtTokenUtil.getUserIdFromToken(token);
        } catch (Exception e) {
            log.warn("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Refresh access token using refresh token
     *
     * <p>This method validates a refresh token and generates a new access token
     * and refresh token pair. The old refresh token is removed from whitelist
     * and blacklisted.
     *
     * @param refreshToken The refresh token to validate
     * @return LoginResponse containing new access token and refresh token
     * @throws BusinessException if refresh token is invalid or expired
     */
    @Transactional
    public LoginResponse refreshToken(String refreshToken) {
        log.info("Refresh token request received");

        // Validate refresh token
        if (!jwtTokenUtil.validateToken(refreshToken)) {
            throw new BusinessException(
                    "Invalid or expired refresh token",
                    UserServiceMessageKeys.REFRESH_TOKEN_INVALID_OR_EXPIRED);
        }

        // Extract JTI from refresh token
        String oldRefreshTokenJti = jwtTokenUtil.getJtiFromToken(refreshToken);
        if (oldRefreshTokenJti == null) {
            throw new BusinessException(
                    "Invalid refresh token - missing JTI",
                    UserServiceMessageKeys.REFRESH_TOKEN_MISSING_JTI);
        }

        // Extract user ID from refresh token (needed for logging)
        Long userId = null;
        try {
            userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
        } catch (Exception e) {
            log.debug("Could not extract userId from refresh token: {}", e.getMessage());
        }

        // Acquire distributed lock to prevent concurrent refresh with the same token.
        // Without this lock, two concurrent requests with the same refresh token can both pass
        // the whitelist check, and the second one destroys the tokens created by the first.
        String refreshLockKey = "refresh:lock:" + oldRefreshTokenJti;
        boolean lockAcquired = false;
        if (redisTemplate != null) {
            try {
                lockAcquired = Boolean.TRUE.equals(
                        redisTemplate.opsForValue().setIfAbsent(refreshLockKey, "1", 30, TimeUnit.SECONDS));
            } catch (Exception e) {
                log.warn("Failed to acquire refresh lock (proceeding without lock): jti={}, error={}", 
                        oldRefreshTokenJti, e.getMessage());
                lockAcquired = true; // Proceed without lock if Redis lock fails
            }
            if (!lockAcquired) {
                // Before rejecting, check if the concurrent request already completed
                // and cached the response. This handles the case where the mobile app sends
                // multiple concurrent refresh requests.
                LoginResponse cachedResponse = getCachedRefreshResponse(oldRefreshTokenJti);
                if (cachedResponse != null) {
                    log.info("Returning cached refresh response for concurrent request: jti={}, userId={}", 
                            oldRefreshTokenJti, userId);
                    return cachedResponse;
                }
                log.warn("Concurrent refresh token request detected and rejected: jti={}, userId={}", 
                        oldRefreshTokenJti, userId);
                throw new BusinessException(
                        "Token refresh already in progress, please retry",
                        UserServiceMessageKeys.REFRESH_TOKEN_REVOKED);
            }
        }

        try {
            // Check if refresh token is whitelisted
            boolean isWhitelisted = tokenWhitelistService.isTokenWhitelisted(oldRefreshTokenJti);
            if (!isWhitelisted) {
                // Grace period - check if a previous refresh already rotated this token.
                // If the client missed the response (network drop, app crash, etc.), the old token
                // is already invalidated but the new tokens were cached for a grace period.
                // Return the cached response instead of rejecting the client (idempotent retry).
                LoginResponse cachedResponse = getCachedRefreshResponse(oldRefreshTokenJti);
                if (cachedResponse != null) {
                    log.info("Returning cached refresh response for previously rotated token: jti={}, userId={} " +
                            "(client likely missed the original response)", oldRefreshTokenJti, userId);
                    return cachedResponse;
                }

                // Log additional context for debugging
                log.warn("Refresh token not found in whitelist: jti={}, userId={}. " +
                        "Possible reasons: token revoked, expired, Redis unavailable, or whitelisting failed. " +
                        "Checking if token is expired...", oldRefreshTokenJti, userId);
                
                // Check if token is expired (more helpful error message)
                try {
                    if (jwtTokenUtil.isTokenExpired(refreshToken)) {
                        log.debug("Refresh token is expired: jti={}, userId={}", oldRefreshTokenJti, userId);
                        throw new BusinessException(
                                "Refresh token has expired",
                                UserServiceMessageKeys.REFRESH_TOKEN_INVALID_OR_EXPIRED);
                    }
                } catch (BusinessException e) {
                    // Re-throw BusinessException (expired token)
                    throw e;
                } catch (Exception e) {
                    // If we can't check expiration, continue with revocation error
                    log.debug("Could not check token expiration: {}", e.getMessage());
                }
                
                throw new BusinessException(
                        "Refresh token has been revoked or is no longer valid",
                        UserServiceMessageKeys.REFRESH_TOKEN_REVOKED);
            }

            // Check if refresh token is blacklisted
            if (isTokenBlacklisted(refreshToken)) {
                log.warn("Refresh token is blacklisted: jti={}, userId={}", oldRefreshTokenJti, userId);
                throw new BusinessException(
                        "Refresh token has been revoked",
                        UserServiceMessageKeys.REFRESH_TOKEN_REVOKED);
            }

            // Ensure userId is available (should be set above, but validate)
            if (userId == null) {
                userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
            }
            if (userId == null) {
                throw new BusinessException(
                        "Invalid refresh token",
                        UserServiceMessageKeys.INVALID_REFRESH_TOKEN);
            }

            // Verify token type is "refresh"
            String tokenType = jwtTokenUtil.getClaimFromToken(refreshToken, claims -> claims.get("type", String.class));
            if (!"refresh".equals(tokenType)) {
                throw new BusinessException(
                        "Invalid token type - refresh token required",
                        UserServiceMessageKeys.REFRESH_TOKEN_INVALID_TYPE);
            }

            // Get user from database
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(
                            "User not found",
                            UserServiceMessageKeys.USER_NOT_FOUND));

            if (!user.getIsActive()) {
                throw new BusinessException(
                        "User account is inactive",
                        UserServiceMessageKeys.USER_ACCOUNT_INACTIVE);
            }

            if (user.getIsBlocked()) {
                throw new BusinessException(
                        "User account is blocked",
                        UserServiceMessageKeys.USER_ACCOUNT_BLOCKED);
            }

            // Get device ID from old token (try metadata first, then token claims)
            String deviceId = null;
            try {
                // Try to get from token metadata first
                var oldTokenMetadata = tokenWhitelistService.getTokenMetadata(oldRefreshTokenJti);
                if (oldTokenMetadata != null) {
                    deviceId = oldTokenMetadata.getDeviceId();
                }
                
                // Fallback: extract from token claims
                if (deviceId == null || deviceId.trim().isEmpty()) {
                    deviceId = jwtTokenUtil.getDeviceIdFromToken(refreshToken);
                }
            } catch (Exception e) {
                log.debug("Could not retrieve device ID from old token: {}", e.getMessage());
            }

            // Remove old refresh token from whitelist
            tokenWhitelistService.removeFromWhitelist(oldRefreshTokenJti);
            log.debug("Removed old refresh token from whitelist: jti={}", oldRefreshTokenJti);
            
            // Fix 2: Revoke only access tokens for the current device instead of ALL user access tokens.
            // Previously revokeAllUserAccessTokens() was used, which invalidated tokens on ALL devices,
            // causing cross-device logout cascades when one device refreshed its token.
            int revokedAccessTokens = 0;
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                revokedAccessTokens = tokenWhitelistService.revokeDeviceAccessTokens(deviceId);
                if (revokedAccessTokens > 0) {
                    log.info("Revoked {} access tokens for device {} during token refresh (userId={})", 
                            revokedAccessTokens, deviceId, user.getUserId());
                }
            } else {
                log.warn("No device ID available during token refresh, skipping device-scoped access token revocation for user: {}",
                        user.getUserId());
            }

            // Generate JTI for new tokens
            String newAccessTokenJti = jwtTokenUtil.generateJti();
            String newRefreshTokenJti = jwtTokenUtil.generateJti();

            // Generate new token pair with pre-generated JTIs and device ID
            String newToken = jwtTokenUtil.generateToken(
                    user.getUserId(),
                    user.getEmail(),
                    newAccessTokenJti,
                    deviceId
            );
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(user.getUserId(), newRefreshTokenJti, deviceId);

            // Get token expiration times
            Long accessTokenExpiration = jwtTokenUtil.getExpirationDateFromToken(newToken).getTime();
            Long refreshTokenExpiration = jwtTokenUtil.getExpirationDateFromToken(newRefreshToken).getTime();

            // Add new tokens to whitelist - verify success
            boolean accessTokenWhitelisted = tokenWhitelistService.addToWhitelist(
                    newAccessTokenJti,
                    user.getUserId(),
                    "access",
                    accessTokenExpiration,
                    deviceId
            );
            boolean refreshTokenWhitelisted = tokenWhitelistService.addToWhitelist(
                    newRefreshTokenJti,
                    user.getUserId(),
                    "refresh",
                    refreshTokenExpiration,
                    deviceId
            );

            // Verify whitelisting succeeded - if Redis is available, whitelisting must succeed
            if (redisTemplate != null) {
                if (!accessTokenWhitelisted) {
                    log.error("Failed to whitelist new access token for user: {}, jti={}", user.getUserId(), newAccessTokenJti);
                    throw new BusinessException(
                            "Failed to whitelist access token - authentication service unavailable",
                            CommonMessageKeys.INTERNAL_SERVER_ERROR);
                }
                if (!refreshTokenWhitelisted) {
                    log.error("Failed to whitelist new refresh token for user: {}, jti={}", user.getUserId(), newRefreshTokenJti);
                    throw new BusinessException(
                            "Failed to whitelist refresh token - authentication service unavailable",
                            CommonMessageKeys.INTERNAL_SERVER_ERROR);
                }
                // Verify tokens are actually in whitelist (defense in depth)
                if (!tokenWhitelistService.isTokenWhitelisted(newAccessTokenJti)) {
                    log.error("New access token whitelist verification failed for user: {}, jti={}", user.getUserId(), newAccessTokenJti);
                    throw new BusinessException(
                            "Token whitelist verification failed - authentication service unavailable",
                            CommonMessageKeys.INTERNAL_SERVER_ERROR);
                }
            } else {
                log.warn("Redis not available - tokens generated but not whitelisted. Token validation may fail in gateway.");
            }

            // Blacklist old refresh token (for enhanced security)
            if (redisTemplate != null) {
                Long expirationTime = jwtTokenUtil.getExpirationDateFromToken(refreshToken).getTime();
                Long currentTime = System.currentTimeMillis();
                Long ttl = expirationTime - currentTime;
                if (ttl > 0) {
                    String blacklistKey = TOKEN_BLACKLIST_PREFIX + refreshToken;
                    long ttlWithBuffer = ttl + 60000; // Add 60s buffer to prevent premature expiration due to clock skew
                    try {
                        redisTemplate.opsForValue().set(blacklistKey, "refreshed", ttlWithBuffer, TimeUnit.MILLISECONDS);
                        log.debug("Old refresh token blacklisted, TTL: {} ms (with buffer)", ttlWithBuffer);
                    } catch (Exception e) {
                        log.warn("Failed to blacklist old refresh token in Redis: {}. Old token may still be usable until expiration.", e.getMessage());
                    }
                }
            }

            log.debug("Generated and whitelisted new tokens for user: {}, accessJti={}, refreshJti={}", 
                    user.getUserId(), newAccessTokenJti, newRefreshTokenJti);

            UserResponse userResponse = userMapper.toResponse(user);

            LoginResponse response = LoginResponse.builder()
                    .token(newToken)
                    .refreshToken(newRefreshToken)
                    .user(userResponse)
                    .expiresIn(jwtExpiration)
                    .build();

            // Cache the refresh response keyed by the old refresh token JTI.
            // If the client misses this response (network drop, app crash, etc.),
            // it can retry with the same old refresh token within the grace period
            // and receive the same new tokens (idempotent retry).
            cacheRefreshResponse(oldRefreshTokenJti, response);

            return response;
        } finally {
            // Release the distributed lock
            if (lockAcquired && redisTemplate != null) {
                try {
                    redisTemplate.delete(refreshLockKey);
                } catch (Exception e) {
                    log.warn("Failed to release refresh lock (will expire in 30s): jti={}, error={}", 
                            oldRefreshTokenJti, e.getMessage());
                }
            }
        }
    }

    /**
     * Extracts device ID from HttpServletRequest.
     *
     * @param request HTTP request
     * @return Device ID as string, or null if not available
     */
    private String extractDeviceIdFromRequest(HttpServletRequest request) {
        if (request == null) {
            return extractDeviceIdFromRequest();
        }
        String deviceId = request.getHeader("X-Device-Id");
        return (deviceId != null && !deviceId.trim().isEmpty()) ? deviceId.trim() : null;
    }

    /**
     * Extracts device ID from request header.
     *
     * @return Device ID as string, or null if not available
     */
    private String extractDeviceIdFromRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String deviceId = request.getHeader("X-Device-Id");
                if (deviceId != null && !deviceId.trim().isEmpty()) {
                    return deviceId.trim();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract device ID: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Caches the refresh response for idempotent retry support.
     * 
     * <p>When a refresh token is rotated, the response is cached for a grace period
     * equal to the access token lifetime. If the client misses the response (network drop,
     * app crash, backgrounding, etc.) and retries with the same old refresh token,
     * the cached response is returned instead of rejecting with REFRESH_TOKEN_REVOKED.
     *
     * <p>The grace period matches the access token lifetime because the client may not
     * attempt to refresh until the access token expires or is rejected by the gateway.
     *
     * @param oldRefreshTokenJti JTI of the old refresh token that was rotated
     * @param response The LoginResponse containing the new tokens
     */
    private void cacheRefreshResponse(String oldRefreshTokenJti, LoginResponse response) {
        if (redisTemplate == null) {
            return;
        }
        try {
            String cacheKey = REFRESH_RESPONSE_CACHE_PREFIX + oldRefreshTokenJti;
            String responseJson = objectMapper.writeValueAsString(response);
            long gracePeriodMs = jwtExpiration; // Match access token lifetime (default: 24h)
            redisTemplate.opsForValue().set(cacheKey, responseJson, gracePeriodMs, TimeUnit.MILLISECONDS);
            log.debug("Cached refresh response for grace period: jti={}, ttl={}ms", 
                    oldRefreshTokenJti, gracePeriodMs);
        } catch (Exception e) {
            log.warn("Failed to cache refresh response (non-critical): jti={}, error={}", 
                    oldRefreshTokenJti, e.getMessage());
        }
    }

    /**
     * Retrieves a cached refresh response for idempotent retry.
     *
     * @param oldRefreshTokenJti JTI of the old refresh token
     * @return Cached LoginResponse if available within grace period, null otherwise
     */
    private LoginResponse getCachedRefreshResponse(String oldRefreshTokenJti) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            String cacheKey = REFRESH_RESPONSE_CACHE_PREFIX + oldRefreshTokenJti;
            String responseJson = redisTemplate.opsForValue().get(cacheKey);
            if (responseJson != null) {
                return objectMapper.readValue(responseJson, LoginResponse.class);
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve cached refresh response: jti={}, error={}", 
                    oldRefreshTokenJti, e.getMessage());
        }
        return null;
    }

    /**
     * Extracts IP address from HttpServletRequest.
     * 
     * <p>Checks X-Forwarded-For header first (for proxy/load balancer),
     * then falls back to remote address.
     *
     * @param request HTTP request
     * @return IP address as string, or null if not available
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return extractIpAddress();
        }

        // Check X-Forwarded-For header (for proxy/load balancer)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            String[] ips = xForwardedFor.split(",");
            return ips[0].trim();
        }
        
        // Check X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        
        // Fallback to remote address
        if (request.getRemoteAddr() != null) {
            return request.getRemoteAddr();
        }
        
        return null;
    }

    /**
     * Extracts IP address from the current HTTP request.
     * 
     * <p>Checks X-Forwarded-For header first (for proxy/load balancer),
     * then falls back to remote address.
     *
     * @return IP address as string, or null if not available
     */
    private String extractIpAddress() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            
            // Check X-Forwarded-For header (for proxy/load balancer)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                String[] ips = xForwardedFor.split(",");
                return ips[0].trim();
            }
            
            // Check X-Real-IP header
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp.trim();
            }
            
            // Fallback to remote address
            if (request.getRemoteAddr() != null) {
                return request.getRemoteAddr();
            }
        } catch (Exception e) {
            log.warn("Error extracting IP address: {}", e.getMessage());
        }
        
        return null;
    }

}
