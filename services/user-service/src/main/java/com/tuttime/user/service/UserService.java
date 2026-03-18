package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.exception.UserServiceMessageKeys;
import com.tuttime.common.kafka.EventProducer;
import com.tuttime.common.kafka.UserEvent;
import com.tuttime.user.dto.UpdateUserProfileRequest;
import com.tuttime.user.dto.UserRegistrationRequest;
import com.tuttime.user.dto.UserResponse;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserDevice;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.UserDeviceRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.repository.readmodel.UserReadModelRepository;
import com.tuttime.user.entity.readmodel.UserReadModel;
import com.tuttime.user.grpc.client.MiningServiceGrpcClient;
import com.tuttime.user.rest.client.RewardsServiceRestClient;
import com.tuttime.user.dto.ReferralInfoResponse;
import com.tuttime.common.proto.GetMiningSpeedResponse;
import com.tuttime.common.proto.ApBalanceResponse;
import com.tuttime.user.service.ImageUrlCacheService;
import com.tuttime.user.service.impl.S3ImageStorageServiceImpl;
import com.tuttime.user.dto.ProfileImageResult;
import java.math.BigDecimal;
import java.util.Optional;
import com.tuttime.user.util.PhoneNumberUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** User Service: handles user registration, profile updates, and user information retrieval with caching and Kafka event publishing. */
@Slf4j
@Service
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EventProducer eventProducer;
    private final AntiFraudService antiFraudService;
    private final UserDeviceRepository userDeviceRepository;
    private final ImageUrlCacheService imageUrlCacheService;
    private final ImageStorageService imageStorageService;
    private final UserReadModelRepository userReadModelRepository;
    private final MiningServiceGrpcClient miningServiceGrpcClient;
    private final RewardsServiceRestClient rewardsServiceRestClient;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AntiFraudService antiFraudService,
            UserDeviceRepository userDeviceRepository,
            ImageUrlCacheService imageUrlCacheService,
            ImageStorageService imageStorageService,
            UserReadModelRepository userReadModelRepository,
            MiningServiceGrpcClient miningServiceGrpcClient,
            RewardsServiceRestClient rewardsServiceRestClient,
            @Autowired(required = false) EventProducer eventProducer) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.antiFraudService = antiFraudService;
        this.userDeviceRepository = userDeviceRepository;
        this.imageUrlCacheService = imageUrlCacheService;
        this.imageStorageService = imageStorageService;
        this.userReadModelRepository = userReadModelRepository;
        this.miningServiceGrpcClient = miningServiceGrpcClient;
        this.rewardsServiceRestClient = rewardsServiceRestClient;
        this.eventProducer = eventProducer;
    }

    /** Registers new user: validates email uniqueness, hashes password, creates user record, publishes USER_CREATED event to Kafka, evicts cache. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    @CacheEvict(value = "users", key = "#result.userId", condition = "#result != null")
    public UserResponse registerUser(UserRegistrationRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());

        // Extract device ID and IP address from request
        String clientDeviceId = extractDeviceIdFromRequest();
        String ip = extractIpAddress();
        HttpServletRequest httpRequest = getHttpServletRequest();

        // Check all uniqueness constraints in a single query (email, phone, username)
        String username = request.getUsername().trim();
        // Normalize phone number for consistent storage and comparison
        String phone = request.getPhone() != null ? PhoneNumberUtil.normalizePhoneNumber(request.getPhone()) : null;
        com.tuttime.user.repository.UserUniquenessCheckProjection uniquenessCheck = 
                userRepository.checkUserUniqueness(request.getEmail(), phone, username);
        
        if (uniquenessCheck != null && uniquenessCheck.isEmailExists()) {
            throw new BusinessException(
                    "User with email " + request.getEmail() + " already exists",
                    UserServiceMessageKeys.EMAIL_ALREADY_REGISTERED);
        }
        
        if (phone != null && !phone.isEmpty() && uniquenessCheck != null && uniquenessCheck.isPhoneExists()) {
            throw new BusinessException(
                    "User with phone number " + phone + " already exists",
                    UserServiceMessageKeys.PHONE_NUMBER_ALREADY_REGISTERED);
        }
        
        if (uniquenessCheck != null && uniquenessCheck.isUsernameExists()) {
            throw new BusinessException(
                    "Username " + username + " is already taken",
                    UserServiceMessageKeys.USERNAME_ALREADY_TAKEN);
        }

        // Validate device registration (before creating user) - device ID is required
        // We need a temporary userId for validation - use 0 as placeholder
        // The actual validation will check if device is used by other users
        try {
            antiFraudService.validateDeviceRegistration(clientDeviceId, ip, 0L);
        } catch (BusinessException e) {
            log.warn("Device registration validation failed during user registration: {}", e.getMessage());
            throw e;
        }

        // Parse dateOfBirth if provided (YYYY-MM-DD format, converted to OffsetDateTime at start of day UTC)
        OffsetDateTime dateOfBirth = null;
        if (request.getDateOfBirth() != null && !request.getDateOfBirth().trim().isEmpty()) {
            try {
                // Parse date string (YYYY-MM-DD) and convert to OffsetDateTime at start of day UTC
                String dateStr = request.getDateOfBirth().trim();
                dateOfBirth = OffsetDateTime.parse(dateStr + "T00:00:00Z");
            } catch (Exception e) {
                log.warn("Invalid dateOfBirth format: {}, ignoring", request.getDateOfBirth());
            }
        }

        // Handle location and privacy level based on whether location is provided
        // Logic: If user sends location info → set to PRIVATE (user wants privacy)
        //        If user doesn't send location → set to EXACT (default auto-detect mode)
        com.tuttime.user.entity.LocationPrivacyLevel privacyLevel = com.tuttime.user.entity.LocationPrivacyLevel.EXACT;
        java.math.BigDecimal latitude = null;
        java.math.BigDecimal longitude = null;
        Integer locationAccuracy = null;
        org.locationtech.jts.geom.Point locationPoint = null;
        Boolean locationSharingEnabled = false;
        OffsetDateTime locationUpdatedAt = null;

        if (request.getLatitude() != null && request.getLongitude() != null) {
            // User sent location info - set privacy level to PRIVATE (user wants privacy)
            log.info("User registration with location: latitude={}, longitude={}. Setting privacy level to PRIVATE.", 
                    request.getLatitude(), request.getLongitude());
            
            try {
                // Validate coordinates
                com.tuttime.user.util.LocationUtils.validateLatitude(request.getLatitude());
                com.tuttime.user.util.LocationUtils.validateLongitude(request.getLongitude());
                if (request.getLocationAccuracy() != null) {
                    com.tuttime.user.util.LocationUtils.validateAccuracy(request.getLocationAccuracy());
                }
                
                // User sent location but wants privacy - set to PRIVATE
                privacyLevel = com.tuttime.user.entity.LocationPrivacyLevel.PRIVATE;
                latitude = request.getLatitude();
                longitude = request.getLongitude();
                locationAccuracy = request.getLocationAccuracy();
                locationPoint = com.tuttime.user.util.LocationUtils.createPoint(latitude, longitude);
                locationSharingEnabled = false; // Privacy enabled, so sharing disabled
                locationUpdatedAt = OffsetDateTime.now(ZoneOffset.UTC);
                
                log.info("User registration: Location stored but privacy level set to PRIVATE (location sharing disabled)");
            } catch (Exception e) {
                log.warn("Invalid location coordinates provided during signup for user: latitude={}, longitude={}, error: {}. Setting privacy level to PRIVATE.", 
                        request.getLatitude(), request.getLongitude(), e.getMessage());
                // If validation fails, set to PRIVATE and don't store invalid location
                privacyLevel = com.tuttime.user.entity.LocationPrivacyLevel.PRIVATE;
                locationSharingEnabled = false;
            }
        } else {
            // User didn't send location info - set privacy level to EXACT (default auto-detect mode)
            log.info("User registration without location - setting privacy level to EXACT (default auto-detect mode)");
            privacyLevel = com.tuttime.user.entity.LocationPrivacyLevel.EXACT;
            // Location fields remain null (default)
        }

        // Create new user
        User user = User.builder()
                .email(request.getEmail())
                .username(username)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(phone)
                .telegram(request.getTelegram())
                .instagram(request.getInstagram())
                .country(request.getCountry())
                .city(request.getCity())
                .dateOfBirth(dateOfBirth)
                .gender(request.getGender())
                // registrationDate is set automatically by @CreatedDate annotation
                .isVerified(false)
                .emailVerified(false)  // Email verification is false by default on signup
                .kycCompleted(false)
                .isActive(true)
                .isBlocked(false)
                // Location fields
                .latitude(latitude)
                .longitude(longitude)
                .locationPoint(locationPoint)
                .locationAccuracy(locationAccuracy)
                .locationSharingEnabled(locationSharingEnabled)
                .locationPrivacyLevel(privacyLevel)
                .locationUpdatedAt(locationUpdatedAt)
                .build();

        user = userRepository.save(user);
        log.info("User registered successfully with ID: {}", user.getUserId());

        // Create device record if device ID is available
        if (clientDeviceId != null && !clientDeviceId.trim().isEmpty()) {
            createUserDevice(user.getUserId(), clientDeviceId, ip, httpRequest);
        }

        // Publish user created event (only if Kafka is available)
        // Note: Event publishing is non-critical - failures should not rollback the registration transaction
        if (eventProducer != null) {
            try {
                // Build metadata with interests if provided
                Map<String, Object> metadata = new HashMap<>();
                if (request.getInterests() != null && !request.getInterests().isEmpty()) {
                    metadata.put("interests", request.getInterests());
                    log.info("Publishing USER_CREATED event with {} interests for userId: {}, interests: {}", 
                            request.getInterests().size(), user.getUserId(), request.getInterests());
                } else {
                    log.info("Publishing USER_CREATED event for userId: {} (no interests provided)", user.getUserId());
                }
                
                UserEvent userCreatedEvent = UserEvent.builder()
                        .eventType(UserEvent.UserEventType.USER_CREATED)
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .isVerified(user.getIsVerified())
                        .kycCompleted(user.getKycCompleted())
                        .isActive(user.getIsActive())
                        .isBlocked(user.getIsBlocked())
                        .metadata(metadata.isEmpty() ? null : metadata)
                        .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                        .build();
                
                eventProducer.publishUserEvent(userCreatedEvent);
                log.debug("Published USER_CREATED event: userId={}, hasInterests={}, metadata={}", 
                        user.getUserId(), metadata.containsKey("interests"), metadata);
            } catch (Exception e) {
                // Kafka publishing failures are non-critical - don't fail registration
                log.error("Error publishing USER_CREATED event during registration (non-critical, continuing registration): userId={}, error={}", 
                        user.getUserId(), e.getMessage(), e);
                // Don't fail registration if event publishing fails - event publishing is non-critical
            }
        } else {
            log.warn("Kafka EventProducer not available - USER_CREATED event will not be published: userId={}", 
                    user.getUserId());
        }

        UserResponse response = userMapper.toResponse(user);
        // Enrich with generated image URL from key
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        return response;
    }

    /**
     * Creates a UserDevice record for a newly registered user.
     * 
     * <p>Device information is extracted from custom headers (preferred) or User-Agent header (fallback):
     * <ul>
     *   <li>X-Device-Type: Device type (iOS, Android, Web) - preferred over User-Agent parsing</li>
     *   <li>X-Device-Model: Device model (e.g., "iPhone 15 Pro", "Samsung Galaxy S24") - preferred over User-Agent parsing</li>
     *   <li>X-OS-Version: OS version (e.g., "iOS 17.1", "Android 14") - preferred over User-Agent parsing</li>
     *   <li>X-App-Version: App version (e.g., "1.0.0")</li>
     *   <li>X-Device-Id: Client device ID (UUID from mobile app)</li>
     *   <li>User-Agent: Fallback for device type, model, and OS version if custom headers not provided</li>
     * </ul>
     */
    private void createUserDevice(Long userId, String clientDeviceId, String ip, HttpServletRequest request) {
        // Declare variables at method scope so they're accessible in catch blocks
        String deviceTypeStr = null;
        UserDevice.DeviceType deviceTypeEnum = null;
        
        try {
            String userAgent = request != null ? request.getHeader("User-Agent") : null;
            
            // Extract device type - prefer custom header, fallback to User-Agent parsing
            deviceTypeStr = request != null ? request.getHeader("X-Device-Type") : null;
            if (deviceTypeStr == null || deviceTypeStr.trim().isEmpty()) {
                deviceTypeStr = extractDeviceType(request);
            }
            log.debug("Device type string received: '{}' (from header: {})", deviceTypeStr, request != null ? request.getHeader("X-Device-Type") : "null");
            deviceTypeEnum = mapDeviceType(deviceTypeStr);
            log.debug("Device type enum mapped to: {}", deviceTypeEnum);
            
            // Extract device model - prefer custom header, fallback to User-Agent parsing
            String deviceModel = request != null ? request.getHeader("X-Device-Model") : null;
            if (deviceModel == null || deviceModel.trim().isEmpty()) {
                deviceModel = extractDeviceModel(userAgent);
            }
            
            // Extract OS version - prefer custom header, fallback to User-Agent parsing
            String osVersion = request != null ? request.getHeader("X-OS-Version") : null;
            if (osVersion == null || osVersion.trim().isEmpty()) {
                osVersion = extractOsVersion(userAgent);
            }
            
            // Extract app version - only from custom header (not available in User-Agent)
            String appVersion = request != null ? request.getHeader("X-App-Version") : null;
            if (appVersion != null && appVersion.trim().isEmpty()) {
                appVersion = null;
            }

            // Generate a placeholder fingerprint for database compatibility (not used for validation)
            String placeholderFingerprint = "placeholder-" + clientDeviceId;

            UserDevice device = UserDevice.builder()
                    .userId(userId)
                    .clientDeviceId(clientDeviceId)
                    .deviceFingerprint(placeholderFingerprint)
                    .deviceType(deviceTypeEnum)
                    .deviceModel(deviceModel)
                    .osVersion(osVersion)
                    .appVersion(appVersion)
                    .ipAddress(ip)
                    .lastSeen(OffsetDateTime.now(ZoneOffset.UTC))
                    .isActive(true)
                    .isTrusted(false)
                    .build();

            log.debug("Attempting to save device entity: userId={}, deviceType={}, deviceTypeClass={}, deviceTypeName={}", 
                    userId, deviceTypeEnum, deviceTypeEnum != null ? deviceTypeEnum.getClass().getName() : "null", 
                    deviceTypeEnum != null ? deviceTypeEnum.name() : "null");
            
            try {
                userDeviceRepository.save(device);
                log.info("Device registered successfully for user: userId={}, clientDeviceId={}, deviceType={}, deviceModel={}, osVersion={}, appVersion={}, ip={}", 
                        userId, clientDeviceId, deviceTypeEnum, deviceModel, osVersion, appVersion, ip);
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                log.error("DataIntegrityViolationException while saving device: userId={}, deviceType={}, deviceTypeEnum={}, error={}, rootCause={}", 
                        userId, deviceTypeStr, deviceTypeEnum, e.getMessage(), 
                        e.getRootCause() != null ? e.getRootCause().getMessage() : "null", e);
                throw e; // Re-throw to trigger transaction rollback
            } catch (org.hibernate.exception.SQLGrammarException e) {
                String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
                log.error("SQLGrammarException while saving device: userId={}, deviceType={}, deviceTypeEnum={}, sqlState={}, error={}, rootCause={}, stackTrace={}", 
                        userId, deviceTypeStr, deviceTypeEnum, e.getSQLState(), e.getMessage(), 
                        e.getCause() != null ? e.getCause().getMessage() : "null", stackTraceSummary);
                throw e; // Re-throw to trigger transaction rollback
            } catch (Exception e) {
                log.error("Unexpected error creating device record: userId={}, clientDeviceId={}, deviceTypeStr={}, deviceTypeEnum={}, errorClass={}, error={}", 
                        userId, clientDeviceId, deviceTypeStr, deviceTypeEnum, 
                        e.getClass().getName(), e.getMessage(), e);
                throw e; // Re-throw to trigger transaction rollback
            }
        } catch (Exception e) {
            log.error("Error creating device record (outer catch): userId={}, clientDeviceId={}, deviceTypeStr={}, deviceTypeEnum={}, errorClass={}, error={}, stackTrace={}", 
                    userId, clientDeviceId, deviceTypeStr, deviceTypeEnum != null ? deviceTypeEnum.name() : "null", 
                    e.getClass().getName(), e.getMessage(), 
                    java.util.Arrays.toString(e.getStackTrace()), e);
            // Don't throw exception - device creation failure shouldn't block registration
            // But log the full error for debugging
        }
    }

    /**
     * Maps device type string to DeviceType enum.
     */
    private UserDevice.DeviceType mapDeviceType(String deviceTypeStr) {
        if (deviceTypeStr == null) {
            return UserDevice.DeviceType.Web;
        }
        
        switch (deviceTypeStr.toLowerCase()) {
            case "ios":
                return UserDevice.DeviceType.iOS;
            case "android":
                return UserDevice.DeviceType.Android;
            case "web":
                return UserDevice.DeviceType.Web;
            default:
                return UserDevice.DeviceType.Web;
        }
    }

    /**
     * Extracts device model from user agent string.
     */
    private String extractDeviceModel(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return null;
        }

        // Simple extraction - can be enhanced with more sophisticated parsing
        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone")) {
            if (ua.contains("iphone15")) return "iPhone 15";
            if (ua.contains("iphone14")) return "iPhone 14";
            if (ua.contains("iphone13")) return "iPhone 13";
            if (ua.contains("iphone12")) return "iPhone 12";
            return "iPhone";
        } else if (ua.contains("ipad")) {
            return "iPad";
        } else if (ua.contains("android")) {
            if (ua.contains("samsung")) return "Samsung Galaxy";
            if (ua.contains("pixel")) return "Google Pixel";
            if (ua.contains("oneplus")) return "OnePlus";
            return "Android Device";
        } else if (ua.contains("chrome")) {
            return "Chrome Browser";
        } else if (ua.contains("firefox")) {
            return "Firefox Browser";
        } else if (ua.contains("safari")) {
            return "Safari Browser";
        }

        return null;
    }

    /**
     * Extracts OS version from user agent string.
     */
    private String extractOsVersion(String userAgent) {
        if (userAgent == null || userAgent.trim().isEmpty()) {
            return null;
        }

        String ua = userAgent;
        
        // Extract iOS version
        if (ua.contains("OS ")) {
            int start = ua.indexOf("OS ");
            int end = ua.indexOf(" ", start + 3);
            if (end == -1) end = ua.indexOf("_", start + 3);
            if (end == -1) end = Math.min(start + 10, ua.length());
            if (start != -1 && end > start) {
                String version = ua.substring(start + 3, end).replace("_", ".");
                return "iOS " + version;
            }
        }
        
        // Extract Android version
        if (ua.contains("Android ")) {
            int start = ua.indexOf("Android ");
            int end = ua.indexOf(";", start);
            if (end == -1) end = ua.indexOf(" ", start + 8);
            if (end == -1) end = Math.min(start + 20, ua.length());
            if (start != -1 && end > start) {
                return ua.substring(start + 8, end).trim();
            }
        }
        
        // Extract Windows version
        if (ua.contains("Windows ")) {
            int start = ua.indexOf("Windows ");
            int end = ua.indexOf(";", start);
            if (end == -1) end = ua.indexOf(" ", start + 8);
            if (end == -1) end = Math.min(start + 20, ua.length());
            if (start != -1 && end > start) {
                return ua.substring(start + 8, end).trim();
            }
        }
        
        // Extract macOS version
        if (ua.contains("Mac OS X ")) {
            int start = ua.indexOf("Mac OS X ");
            int end = ua.indexOf(";", start);
            if (end == -1) end = ua.indexOf(" ", start + 9);
            if (end == -1) end = Math.min(start + 20, ua.length());
            if (start != -1 && end > start) {
                return "macOS " + ua.substring(start + 9, end).trim();
            }
        }

        return null;
    }

    /**
     * Extracts device ID from request header.
     */
    private String extractDeviceIdFromRequest() {
        try {
            HttpServletRequest request = getHttpServletRequest();
            if (request != null) {
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
     * Extracts IP address from request.
     */
    private String extractIpAddress() {
        try {
            HttpServletRequest request = getHttpServletRequest();
            if (request == null) {
                return null;
            }

            // Check X-Forwarded-For header
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                String[] ips = xForwardedFor.split(",");
                return ips[0].trim();
            }

            // Check X-Real-IP header
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp.trim();
            }

            // Fallback to remote address
            return request.getRemoteAddr();
        } catch (Exception e) {
            log.debug("Could not extract IP address: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Gets the current user by ID.
     *
     * @param userId User ID
     * @return UserResponse containing user information
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId")
    public UserResponse getCurrentUser(Long userId) {
        log.debug("Fetching current user: {}", userId);
        return getUserById(userId);
    }

    /**
     * Updates user profile information.
     *
     * @param userId User ID
     * @param request Update request containing new profile data
     * @return Updated UserResponse
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException if email is already taken by another user
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateUserProfile(Long userId, com.tuttime.user.dto.UpdateUserProfileRequest request) {
        log.info("Updating profile for user: {}", userId);

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update fields if provided
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        // Username is required, so it must be provided
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("Username is required");
        }
        String username = request.getUsername().trim();
        // Check if username is already taken by another user (case-insensitive comparison)
        if (!username.equalsIgnoreCase(user.getUsername())) {
            userRepository.findByUsername(username)
                    .ifPresent(existingUser -> {
                        if (!existingUser.getUserId().equals(userId)) {
                            throw new BusinessException("Username is already taken");
                        }
                    });
        }
        user.setUsername(username);
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email is already taken by another user
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getUserId().equals(userId)) {
                            throw new BusinessException("Email is already registered to another account");
                        }
                    });
            user.setEmail(request.getEmail());
        }
        if (request.getTelegramId() != null) {
            user.setTelegram(request.getTelegramId());
        }
        if (request.getInstagram() != null) {
            user.setInstagram(request.getInstagram().trim());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAbout() != null) {
            user.setAbout(request.getAbout().trim());
        }
        if (request.getWebsites() != null) {
            user.setWebsites(request.getWebsites().trim());
        }
        if (request.getPreferredLanguage() != null) {
            String language = request.getPreferredLanguage().trim();
            // Validate language code format (basic validation)
            if (language.length() > 10) {
                throw new BusinessException("Preferred language code must not exceed 10 characters");
            }
            user.setPreferredLanguage(language.isEmpty() ? "en" : language);
        }

        user = userRepository.save(user);
        
        // Flush to ensure the profile is written to database immediately
        userRepository.flush();
        
        log.info("Profile updated successfully for user: {}, savedToDb=true", userId);

        try {
            UserResponse response = userMapper.toResponse(user);
            // Enrich with generated image URL from key
            userMapper.enrichImageUrl(response, imageUrlCacheService);
            log.debug("Successfully mapped user to response for userId: {}", userId);
            return response;
        } catch (Exception e) {
            log.error("Failed to map user to response for userId: {}", userId, e);
            throw new BusinessException("Failed to generate user response: " + e.getMessage());
        }
    }

    /**
     * Updates user profile with both profile data and image key.
     * 
     * <p>Stores the image key (e.g., "users/123/abc.jpg") in the database.
     * URLs are generated on-demand when serving images or in API responses to avoid expiration issues.
     * 
     * <p>For backward compatibility, this method also accepts full URLs (legacy data).
     * 
     * @param userId User ID
     * @param request Update request containing profile data
     * @param imageKey Image key (e.g., "users/123/abc.jpg") or URL (for backward compatibility)
     * @return Updated UserResponse
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateUserProfileWithImage(Long userId, UpdateUserProfileRequest request, String imageKey) {
        log.info("Updating profile with image for user: {}", userId);

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update profile fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            // Check if email is already taken by another user
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getUserId().equals(userId)) {
                            throw new BusinessException("Email is already registered to another account");
                        }
                    });
            user.setEmail(request.getEmail());
        }
        if (request.getTelegramId() != null) {
            user.setTelegram(request.getTelegramId());
        }
        if (request.getInstagram() != null) {
            user.setInstagram(request.getInstagram().trim());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }

        // Generate URL from key and store URL in database
        String imageUrl = imageStorageService.getImageUrl(imageKey);
        user.setProfileImageUrl(imageUrl);

        if (request.getAbout() != null) {
            user.setAbout(request.getAbout().trim());
        }
        if (request.getWebsites() != null) {
            user.setWebsites(request.getWebsites().trim());
        }

        // Username is required, so it must be provided
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("Username is required");
        }
        String username = request.getUsername().trim();
        // Check if username is already taken by another user (case-insensitive comparison)
        if (!username.equalsIgnoreCase(user.getUsername())) {
            userRepository.findByUsername(username)
                    .ifPresent(existingUser -> {
                        if (!existingUser.getUserId().equals(userId)) {
                            throw new BusinessException("Username is already taken");
                        }
                    });
        }
        user.setUsername(username);

        user = userRepository.save(user);
        
        // Flush to ensure the profile is written to database immediately
        userRepository.flush();
        
        log.info("Profile updated with image successfully for user: {}, savedToDb=true", userId);

        try {
            UserResponse response = userMapper.toResponse(user);
            // Enrich with image URL (for backward compatibility with legacy keys)
            userMapper.enrichImageUrl(response, imageUrlCacheService);
            log.debug("Successfully mapped user to response for userId: {} (with image)", userId);
            return response;
        } catch (Exception e) {
            log.error("Failed to map user to response for userId: {} (with image)", userId, e);
            throw new BusinessException("Failed to generate user response: " + e.getMessage());
        }
    }

    /**
     * Updates user profile with both profile data and multi-size images.
     * 
     * <p>Stores all thumbnail sizes and original image URLs in database.
     * This enables optimized performance across various use cases:
     * <ul>
     *   <li>Tiny (32x32px, ~1-3KB) - for map markers, compact lists, small avatars</li>
     *   <li>Small (128x128px, ~5-10KB) - for chat headers, profile cards, medium avatars</li>
     *   <li>Medium (512x512px, ~30-50KB) - for profile previews, modals, large cards</li>
     *   <li>Original (1024x1024px, ~100-300KB) - for full profile view, detailed display</li>
     * </ul>
     * 
     * @param userId User ID
     * @param request Update request containing profile data
     * @param imageResult Result containing all image size keys
     * @return Updated UserResponse
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateUserProfileWithMultiSizeImage(Long userId, UpdateUserProfileRequest request, ProfileImageResult imageResult) {
        log.info("Updating profile with multi-size image for user: {}", userId);

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Update profile fields
        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName().trim());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName().trim());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            userRepository.findByEmail(request.getEmail())
                    .ifPresent(existingUser -> {
                        if (!existingUser.getUserId().equals(userId)) {
                            throw new BusinessException("Email is already registered to another account");
                        }
                    });
            user.setEmail(request.getEmail());
        }
        if (request.getTelegramId() != null) {
            user.setTelegram(request.getTelegramId());
        }
        if (request.getInstagram() != null) {
            user.setInstagram(request.getInstagram().trim());
        }
        if (request.getCountry() != null) {
            user.setCountry(request.getCountry());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getAbout() != null) {
            user.setAbout(request.getAbout().trim());
        }
        if (request.getWebsites() != null) {
            user.setWebsites(request.getWebsites().trim());
        }

        // Generate URLs from keys for all sizes and store in database
        String originalUrl = imageStorageService.getImageUrl(imageResult.getOriginalKey());
        String tinyUrl = imageStorageService.getImageUrl(imageResult.getTinyKey());
        String smallUrl = imageStorageService.getImageUrl(imageResult.getSmallKey());
        String mediumUrl = imageStorageService.getImageUrl(imageResult.getMediumKey());
        
        user.setProfileImageUrl(originalUrl);
        user.setProfileImageThumbnailUrl(tinyUrl);
        user.setProfileImageSmallUrl(smallUrl);
        user.setProfileImageMediumUrl(mediumUrl);
        
        log.info("Profile images set for user {}: tiny={}, small={}, medium={}, original={}", 
                userId, imageResult.getTinyKey(), imageResult.getSmallKey(), 
                imageResult.getMediumKey(), imageResult.getOriginalKey());

        // Username is required
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new BusinessException("Username is required");
        }
        String username = request.getUsername().trim();
        if (!username.equalsIgnoreCase(user.getUsername())) {
            userRepository.findByUsername(username)
                    .ifPresent(existingUser -> {
                        if (!existingUser.getUserId().equals(userId)) {
                            throw new BusinessException("Username is already taken");
                        }
                    });
        }
        user.setUsername(username);

        user = userRepository.save(user);
        userRepository.flush();
        
        log.info("Profile updated with multi-size image successfully for user: {}", userId);

        try {
            UserResponse response = userMapper.toResponse(user);
            userMapper.enrichImageUrl(response, imageUrlCacheService);
            return response;
        } catch (Exception e) {
            log.error("Failed to map user to response for userId: {} (with multi-size image)", userId, e);
            throw new BusinessException("Failed to generate user response: " + e.getMessage());
        }
    }

    /**
     * Updates user profile image.
     * 
     * <p>Stores the presigned URL (generated at upload time) in the database.
     * URLs are generated once when uploading, then saved and returned directly without regeneration.
     *
     * @param userId User ID
     * @param imageUrl Presigned URL (generated at upload time)
     * @return Updated UserResponse
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateProfileImage(Long userId, String imageUrl) {
        log.info("Updating profile image for user: {}", userId);

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Store presigned URL in database (generated at upload time, returned directly without regeneration)
        user.setProfileImageUrl(imageUrl);
        user = userRepository.save(user);
        
        // Flush to ensure the profile image is written to database immediately
        userRepository.flush();
        
        log.info("Profile image updated successfully for user: {}, savedToDb=true", userId);
        UserResponse response = userMapper.toResponse(user);
        // No URL regeneration - return URL from database directly
        return response;
    }

    /**
     * Updates user profile image with multiple thumbnail sizes.
     * 
     * <p>Stores all size URLs in database for optimized performance across various use cases:
     * <ul>
     *   <li>Tiny (32x32px, ~1-3KB) - for map markers, compact lists, small avatars</li>
     *   <li>Small (128x128px, ~5-10KB) - for chat headers, profile cards, medium avatars</li>
     *   <li>Medium (512x512px, ~30-50KB) - for profile previews, modals, large cards</li>
     *   <li>Original (1024x1024px, ~100-300KB) - for full profile view, detailed display</li>
     * </ul>
     *
     * @param userId User ID
     * @param originalUrl URL for original image (1024x1024px)
     * @param tinyUrl URL for tiny thumbnail (32x32px)
     * @param smallUrl URL for small thumbnail (128x128px)
     * @param mediumUrl URL for medium thumbnail (512x512px)
     * @return Updated UserResponse
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserResponse updateProfileImageWithThumbnail(Long userId, String originalUrl, String tinyUrl, String smallUrl, String mediumUrl) {
        log.info("Updating profile image with multiple thumbnails for user: {}", userId);

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Store all size URLs in database
        user.setProfileImageUrl(originalUrl);
        user.setProfileImageThumbnailUrl(tinyUrl);
        user.setProfileImageSmallUrl(smallUrl);
        user.setProfileImageMediumUrl(mediumUrl);
        user = userRepository.save(user);
        
        userRepository.flush();
        
        log.info("Profile image with all thumbnails updated successfully for user: {}", userId);
        UserResponse response = userMapper.toResponse(user);
        return response;
    }

    /**
     * Admin: Get device by client device ID or numeric device ID
     * 
     * <p>This method tries to find the device by:
     * 1. clientDeviceId (UUID from mobile app) - primary method
     * 2. deviceId (numeric ID) - fallback if clientDeviceId is numeric
     * 
     * @param clientDeviceId Client device ID (UUID from mobile app) or numeric device ID
     * @return UserDevice if found, null otherwise
     */
    @Transactional(readOnly = true)
    public UserDevice getDeviceByClientDeviceId(String clientDeviceId) {
        if (clientDeviceId == null || clientDeviceId.trim().isEmpty()) {
            return null;
        }
        
        // Try to find by clientDeviceId first
        UserDevice device = userDeviceRepository.findByClientDeviceId(clientDeviceId).orElse(null);
        
        // If not found and clientDeviceId is numeric, try to find by deviceId
        if (device == null) {
            try {
                Long deviceId = Long.parseLong(clientDeviceId);
                device = userDeviceRepository.findById(deviceId).orElse(null);
            } catch (NumberFormatException e) {
                // Not a number, continue
            }
        }
        
        return device;
    }

    /**
     * Admin: Get devices by IP address
     * 
     * @param ipAddress IP address to search for
     * @return List of devices with the specified IP address
     */
    @Transactional(readOnly = true)
    public List<UserDevice> getDevicesByIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return List.of();
        }
        return userDeviceRepository.findByIpAddress(ipAddress);
    }

    /**
     * Admin: Get blocked devices (isActive = false) with pagination
     * 
     * @param pageable Pagination parameters
     * @return Page of blocked devices
     */
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<UserDevice> getBlockedDevices(org.springframework.data.domain.Pageable pageable) {
        return userDeviceRepository.findByIsActiveFalse(pageable);
    }

    /**
     * Admin: Update device status (block/unblock device)
     * 
     * <p>This method tries to find the device by:
     * 1. clientDeviceId (UUID from mobile app) - primary method
     * 2. deviceId (numeric ID) - fallback if clientDeviceId is numeric
     * 
     * @param clientDeviceId Client device ID (UUID from mobile app) or numeric device ID
     * @param isActive Whether the device should be active
     * @param isTrusted Whether the device should be trusted
     * @return Updated UserDevice if found, null otherwise
     */
    @Transactional
    public UserDevice updateDeviceStatus(String clientDeviceId, Boolean isActive, Boolean isTrusted) {
        if (clientDeviceId == null || clientDeviceId.trim().isEmpty()) {
            log.warn("updateDeviceStatus: clientDeviceId is null or empty");
            return null;
        }
        
        log.info("updateDeviceStatus: Looking for device with clientDeviceId={}, isActive={}, isTrusted={}", 
                clientDeviceId, isActive, isTrusted);
        
        UserDevice device = null;
        
        // Try to find by clientDeviceId first
        device = userDeviceRepository.findByClientDeviceId(clientDeviceId).orElse(null);
        
        // If not found and clientDeviceId is numeric, try to find by deviceId
        if (device == null) {
            try {
                Long deviceId = Long.parseLong(clientDeviceId);
                device = userDeviceRepository.findById(deviceId).orElse(null);
                if (device != null) {
                    log.info("updateDeviceStatus: Found device by numeric deviceId={}", deviceId);
                }
            } catch (NumberFormatException e) {
                // Not a number, continue
            }
        }
        
        if (device == null) {
            log.warn("updateDeviceStatus: Device not found with clientDeviceId={} (tried both clientDeviceId and numeric deviceId)", clientDeviceId);
            return null;
        }
        
        log.info("updateDeviceStatus: Found device - deviceId={}, userId={}, clientDeviceId={}, current isActive={}, current isTrusted={}", 
                device.getDeviceId(), device.getUserId(), device.getClientDeviceId(), device.getIsActive(), device.getIsTrusted());
        
        boolean changed = false;
        if (isActive != null && !isActive.equals(device.getIsActive())) {
            device.setIsActive(isActive);
            changed = true;
            log.info("updateDeviceStatus: Setting isActive to {}", isActive);
        }
        if (isTrusted != null && !isTrusted.equals(device.getIsTrusted())) {
            device.setIsTrusted(isTrusted);
            changed = true;
            log.info("updateDeviceStatus: Setting isTrusted to {}", isTrusted);
        }
        
        if (changed) {
            UserDevice saved = userDeviceRepository.save(device);
            // Flush to ensure immediate persistence
            userDeviceRepository.flush();
            log.info("updateDeviceStatus: Device saved and flushed - deviceId={}, isActive={}, isTrusted={}", 
                    saved.getDeviceId(), saved.getIsActive(), saved.getIsTrusted());
            return saved;
        } else {
            log.info("updateDeviceStatus: No changes needed - deviceId={}", device.getDeviceId());
            return device;
        }
    }

    /**
     * Changes user password.
     *
     * @param userId User ID
     * @param oldPassword Current password
     * @param newPassword New password
     * @throws ResourceNotFoundException if user not found
     * @throws BusinessException if old password is incorrect or new password is invalid
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));

        // Verify old password
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BusinessException("Current password is incorrect");
        }

        // Validate new password
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException("New password is required");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("New password must be at least 6 characters");
        }

        // Check if new password is same as current password
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("New password must be different from current password");
        }

        // Hash and update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    /**
     * Checks if a phone number is already registered.
     *
     * @param phoneNumber Phone number to check
     * @return true if phone number is registered, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isPhoneRegistered(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        // Normalize phone number before lookup for consistent comparison
        String normalizedPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);
        // Check including deleted accounts to prevent re-registration with same phone number
        return userRepository.findByPhoneIncludingDeleted(normalizedPhone).isPresent();
    }

    /**
     * Checks if a username is already taken.
     * 
     * @param username Username to check
     * @return true if username exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isUsernameTaken(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        return userRepository.findByUsername(username.trim()).isPresent();
    }

    /**
     * Checks if an email is already registered.
     * 
     * @param email Email to check
     * @return true if email exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return userRepository.findByEmail(email.trim().toLowerCase()).isPresent();
    }

    /**
     * Extracts device type from request headers.
     */
    private String extractDeviceType(HttpServletRequest request) {
        if (request == null) {
            return "Unknown";
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) {
            return "Unknown";
        }

        String ua = userAgent.toLowerCase();
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            return "iOS";
        } else if (ua.contains("android")) {
            return "Android";
        } else if (ua.contains("windows") || ua.contains("mac") || ua.contains("linux")) {
            return "Web";
        }

        return "Unknown";
    }

    /**
     * Gets the current HTTP request from Spring context.
     */
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                return attributes.getRequest();
            }
        } catch (Exception e) {
            log.debug("Could not get HTTP request: {}", e.getMessage());
        }
        return null;
    }

    /**
     * Gets user information by ID (with caching).
     *
     * <p>This method is cached using Redis. Returns user information
     * excluding soft-deleted users.
     *
     * <p><b>Cache Key:</b> "users:{userId}"
     * <p><b>Cache TTL:</b> Configured in RedisConfig
     *
     * @param userId The ID of the user
     * @return UserResponse containing user information
     * @throws ResourceNotFoundException if user doesn't exist or is deleted
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public UserResponse getUserById(Long userId) {
        log.debug("Fetching user with ID: {} (cache miss)", userId);
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        
        // Try to get from read model first (contains aggregated data like balances)
        Optional<UserReadModel> readModelOpt = userReadModelRepository.findByIdAndNotDeleted(userId);
        
        UserResponse response;
        if (readModelOpt.isPresent()) {
            // Use read model (fast path with aggregated data)
            UserReadModel readModel = readModelOpt.get();
            response = userMapper.toResponse(readModel);
            // Set balances from read model
            response.setTotalApartBalance(readModel.getTotalApartBalance());
            response.setTotalApBalance(readModel.getTotalApBalance());
        } else {
            // Fallback: user exists but not in read model
            log.debug("User {} not found in read model, using main User table", userId);
            response = userMapper.toResponse(user);
            // Set default values for balances
            response.setTotalApartBalance(BigDecimal.ZERO);
            response.setTotalApBalance(BigDecimal.ZERO);
        }
        
        // Always enrich with data from main User table (for blocked users, etc.)
        response.setPhone(user.getPhone());
        response.setTelegram(user.getTelegram());
        response.setCountry(user.getCountry());
        response.setCity(user.getCity());
        response.setKycVerifiedAt(user.getKycVerifiedAt());
        response.setBlockedReason(user.getBlockedReason());
        response.setBlockedAt(user.getBlockedAt());
        response.setIsBlocked(user.getIsBlocked());
        
        // Enrich with generated image URL from key
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        
        // Enrich with current mining speed, current AP balance, and friends count
        enrichWithMiningAndFriendsData(response, userId);
        
        return response;
    }
    
    /** Enriches user response with current mining speed, current AP balance, and friends count from external services. */
    private void enrichWithMiningAndFriendsData(UserResponse response, Long userId) {
        // Fetch current mining speed
        try {
            GetMiningSpeedResponse miningSpeedResponse = miningServiceGrpcClient.getMiningSpeed(userId);
            if (miningSpeedResponse != null) {
                response.setCurrentMiningSpeed(BigDecimal.valueOf(miningSpeedResponse.getTotalSpeed()));
            } else {
                response.setCurrentMiningSpeed(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.warn("Failed to get mining speed for user {}: {}", userId, e.getMessage());
            response.setCurrentMiningSpeed(BigDecimal.ZERO);
        }
        
        // Fetch current AP balance
        try {
            ApBalanceResponse apBalanceResponse = miningServiceGrpcClient.getApBalance(userId);
            if (apBalanceResponse != null) {
                response.setCurrentApBalance(new BigDecimal(apBalanceResponse.getCurrentBalance()));
            } else {
                response.setCurrentApBalance(BigDecimal.ZERO);
            }
        } catch (Exception e) {
            log.warn("Failed to get AP balance for user {}: {}", userId, e.getMessage());
            response.setCurrentApBalance(BigDecimal.ZERO);
        }
        
        // Fetch total friends count (total referrals)
        try {
            ReferralInfoResponse referralInfo = rewardsServiceRestClient.getReferralInfo(userId);
            if (referralInfo != null && referralInfo.getTotalReferrals() != null) {
                response.setTotalFriendsCount(referralInfo.getTotalReferrals());
            } else {
                response.setTotalFriendsCount(0);
            }
        } catch (Exception e) {
            log.warn("Failed to get friends count for user {}: {}", userId, e.getMessage());
            response.setTotalFriendsCount(0);
        }
    }

    /**
     * Updates the last active timestamp for a user.
     *
     * <p>This method updates the user's last active timestamp to the current time.
     * Used to track user activity on the platform.
     *
     * @param userId The ID of the user
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public void updateLastActive(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastActive(OffsetDateTime.now(ZoneOffset.UTC));
            userRepository.save(user);
        });
    }

    /**
     * Updates the last login timestamp for a user.
     *
     * <p>This method updates both the last login and last active timestamps
     * to the current time. Called when a user successfully logs in.
     *
     * @param userId The ID of the user
     */
    /**
     * Updates the last login timestamp for a user.
     * 
     * <p>This method updates both the last login and last active timestamps
     * to the current time. Called when a user successfully logs in.
     * 
     * <p><b>Note:</b> This method does not have @Transactional annotation
     * because it's always called from within an existing transaction.
     * Removing @Transactional prevents the ReadOnlyTransactionInterceptor
     * from interfering with nested transactions.
     *
     * @param userId The ID of the user
     */
    @CacheEvict(value = "users", key = "#userId")
    public void updateLastLogin(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setLastLogin(OffsetDateTime.now(ZoneOffset.UTC));
            user.setLastActive(OffsetDateTime.now(ZoneOffset.UTC));
            userRepository.save(user);
        });
    }

    /**
     * Get FCM tokens for a user's active devices
     * 
     * <p>This method is used by notification-service to fetch FCM tokens
     * for sending push notifications to a user's active devices.
     * 
     * @param userId User ID
     * @return List of FCM tokens for active devices
     */
    @Transactional(readOnly = true)
    public List<String> getUserFcmTokens(Long userId) {
        log.debug("Fetching FCM tokens for user: userId={}", userId);
        return userDeviceRepository.findFcmTokensByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Update FCM token for a user's device
     * 
     * <p>This method updates the FCM token for the user's device.
     * The device is identified by clientDeviceId (from header) or device fingerprint.
     * 
     * @param userId User ID
     * @param fcmToken FCM registration token
     * @param clientDeviceId Optional client device ID (from X-Device-Id header)
     * @return Updated UserDevice
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public UserDevice updateFcmToken(Long userId, String fcmToken, String clientDeviceId) {
        log.info("Updating FCM token for user: userId={}, clientDeviceId={}", userId, clientDeviceId);
        
        UserDevice device = null;
        
        // Try to find device by clientDeviceId first
        if (clientDeviceId != null && !clientDeviceId.trim().isEmpty()) {
            device = userDeviceRepository.findByUserIdAndClientDeviceId(userId, clientDeviceId)
                    .orElse(null);
        }
        
        // Device lookup is now only by clientDeviceId
        
        // If still not found, get the most recent active device for the user
        if (device == null) {
            List<UserDevice> devices = userDeviceRepository.findByUserIdAndIsActiveTrue(userId);
            if (!devices.isEmpty()) {
                // Get the most recently used device
                device = devices.stream()
                        .max((d1, d2) -> d1.getLastSeen().compareTo(d2.getLastSeen()))
                        .orElse(devices.get(0));
            }
        }
        
        if (device == null) {
            log.warn("No device found for user: userId={}, cannot update FCM token", userId);
            throw new BusinessException(
                    "No active device found for user. Please ensure device is registered.",
                    UserServiceMessageKeys.DEVICE_NOT_FOUND);
        }
        
        device.setFcmToken(fcmToken);
        device.setLastSeen(OffsetDateTime.now(ZoneOffset.UTC));
        device = userDeviceRepository.save(device);
        
        log.info("FCM token updated successfully: userId={}, deviceId={}, clientDeviceId={}", 
                userId, device.getDeviceId(), device.getClientDeviceId());
        
        return device;
    }
    
    /**
     * Remove invalid FCM tokens for a user
     * 
     * <p>This method is called by notification-service when FCM returns authentication errors (401)
     * or other invalid token errors. It removes the FCM tokens from the user's devices.
     * 
     * @param userId User ID
     * @param invalidTokens List of invalid FCM tokens to remove
     * @return Number of tokens removed
     */
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    public int removeInvalidFcmTokens(Long userId, List<String> invalidTokens) {
        if (invalidTokens == null || invalidTokens.isEmpty()) {
            log.debug("No invalid FCM tokens to remove for user: userId={}", userId);
            return 0;
        }
        
        log.info("Removing {} invalid FCM token(s) for user: userId={}", invalidTokens.size(), userId);
        
        int removedCount = 0;
        for (String invalidToken : invalidTokens) {
            List<UserDevice> devices = userDeviceRepository.findByUserIdAndFcmToken(userId, invalidToken);
            for (UserDevice device : devices) {
                device.setFcmToken(null);
                userDeviceRepository.save(device);
                removedCount++;
                log.debug("Removed invalid FCM token from device: userId={}, deviceId={}, clientDeviceId={}", 
                        userId, device.getDeviceId(), device.getClientDeviceId());
            }
        }
        
        log.info("Removed {} invalid FCM token(s) for user: userId={}", removedCount, userId);
        return removedCount;
    }

    /**
     * Deletes the current user's account (soft delete).
     * 
     * @param userId The ID of the user deleting their account
     * @throws ResourceNotFoundException if user not found
     */
    @Transactional
    @CacheEvict(value = {"users", "userDetails"}, key = "#userId")
    public void deleteMyAccount(Long userId) {
        log.info("User requesting account deletion: userId={}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.getDeletedAt() != null) {
            log.warn("User account already deleted: userId={}", userId);
            return;
        }
        
        // Soft delete: set deletedAt and deactivate
        OffsetDateTime deletedAt = OffsetDateTime.now(ZoneOffset.UTC);
        user.setDeletedAt(deletedAt);
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("User account deleted (soft delete): userId={}, deletedAt={}", userId, deletedAt);
        
        // Publish USER_DEACTIVATED event after transaction commits
        // This allows other services to react (stop mining, etc.)
        if (eventProducer != null) {
            org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            try {
                                UserEvent event = UserEvent.builder()
                                        .eventType(UserEvent.UserEventType.USER_DEACTIVATED)
                                        .userId(userId)
                                        .email(user.getEmail())
                                        .username(user.getUsername())
                                        .isVerified(user.getIsVerified())
                                        .kycCompleted(user.getKycCompleted())
                                        .isActive(false)
                                        .isBlocked(user.getIsBlocked())
                                        .timestamp(deletedAt)
                                        .build();
                                
                                eventProducer.publishUserEvent(event);
                                log.info("Published USER_DEACTIVATED event for user: userId={}", userId);
                            } catch (Exception e) {
                                log.error("Failed to publish USER_DEACTIVATED event for user {}: {}", 
                                        userId, e.getMessage(), e);
                                // Don't fail deletion if event publishing fails
                            }
                        }
                    }
            );
        }
    }
}

