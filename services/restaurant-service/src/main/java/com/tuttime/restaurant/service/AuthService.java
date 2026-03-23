package com.tuttime.restaurant.service;

import com.tuttime.common.kafka.EventProducer;
import com.tuttime.common.kafka.RestaurantEvent;
import com.tuttime.restaurant.dto.RestaurantLoginResponse;
import com.tuttime.restaurant.dto.RestaurantRegistrationRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.exception.RestaurantServiceMessageKeys;
import com.tuttime.restaurant.mapper.RestaurantMapper;
import com.tuttime.restaurant.repository.RestaurantAuditLogRepository;
import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.util.PhoneNumberUtil;
import com.tuttime.restaurant.util.RestaurantJwtTokenUtil;
import com.tuttime.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
//@RequiredArgsConstructor
public class AuthService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;;
    private final RestaurantAuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final EventProducer eventProducer;
    private final RestaurantJwtTokenUtil jwtTokenUtil;
    private final AuditService auditService;

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;
    private static final int LOCKOUT_DURATION_MINUTES = 30;

    public AuthService(
            RestaurantRepository restaurantRepository,
            RestaurantMapper restaurantMapper,
            RestaurantAuditLogRepository auditLogRepository,
            PasswordEncoder passwordEncoder,
            org.springframework.beans.factory.ObjectProvider<EventProducer> eventProducerProvider, // Using ObjectProvider is safer
            RestaurantJwtTokenUtil jwtTokenUtil,
            AuditService auditService
    ) {
        this.restaurantRepository = restaurantRepository;
        this.restaurantMapper = restaurantMapper;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventProducer = eventProducerProvider.getIfAvailable(); // Null if bean not found
        this.jwtTokenUtil = jwtTokenUtil;
        this.auditService = auditService;
    }

    /** Registers new restaurant: validates email uniqueness, hashes password, creates record, publishes USER_CREATED event to Kafka, evicts cache. */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRED)
    @CacheEvict(value = "users", key = "#result.userId", condition = "#result != null")
    public RestaurantResponse registerRestaurant(
            RestaurantRegistrationRequest registrationRequest) {
        log.info("Registering new user with email: {}", registrationRequest.getEmail());

        // Extract device ID and IP address from request
        String ip = extractIpAddress();
        HttpServletRequest httpRequest = getHttpServletRequest();

        // Check all uniqueness constraints in a single query (email, phone, username)
        String email = registrationRequest.getEmail().trim();
        String name = registrationRequest.getName().trim();
        String phone = registrationRequest.getPhone() != null ? PhoneNumberUtil.normalizePhoneNumber(registrationRequest.getPhone()) : null;

        com.tuttime.restaurant.repository.RestaurantUniquenessCheckProjection uniquenessCheck =
                restaurantRepository.checkRestaurantUniqueness(email, phone, name);

        if (uniquenessCheck != null && uniquenessCheck.isEmailExists()) {
            throw new BusinessException(
                    "User with email " + registrationRequest.getEmail() + " already exists",
                    RestaurantServiceMessageKeys.EMAIL_ALREADY_REGISTERED);
        }

        if (phone != null && !phone.isEmpty() && uniquenessCheck != null && uniquenessCheck.isPhoneExists()) {
            throw new BusinessException(
                    "User with phone number " + phone + " already exists",
                    RestaurantServiceMessageKeys.PHONE_NUMBER_ALREADY_REGISTERED);
        }

        if (uniquenessCheck != null && uniquenessCheck.isNameExists()) {
            throw new BusinessException(
                    "name " + name + " is already taken",
                    RestaurantServiceMessageKeys.USERNAME_ALREADY_TAKEN);
        }

        // Create new user
        Restaurant restaurant = Restaurant.builder()
                .email(registrationRequest.getEmail())
                .name(name)
                .passwordHash(passwordEncoder.encode(registrationRequest.getPassword()))
                .phone(phone)
                .country(registrationRequest.getCountry())
                .city(registrationRequest.getCity())
                // registrationDate is set automatically by @CreatedDate annotation
                .isVerified(false)
                .isActive(true)
                .isBlocked(false)
                .build();

        restaurant = restaurantRepository.save(restaurant);
        log.info("User registered successfully with ID: {}", restaurant.getRestaurantId());


        // Publish user created event (only if Kafka is available)
        // Note: Event publishing is non-critical - failures should not rollback the registration transaction
        if (eventProducer != null) {
            try {
                // Build metadata with interests if provided
                Map<String, Object> metadata = new HashMap<>();
                log.info("Publishing USER_CREATED event for userId: {} (no interests provided)", restaurant.getRestaurantId());

                RestaurantEvent restaurantCreatedEvent = RestaurantEvent.builder()
                        .eventType(RestaurantEvent.RestaurantEventType.RESTAURANT_CREATED)
                        .restaurantId(restaurant.getRestaurantId())
                        .email(restaurant.getEmail())
                        .name(restaurant.getName())
                        .isVerified(restaurant.getIsVerified())
                        .isActive(restaurant.getIsActive())
                        .isBlocked(restaurant.getIsBlocked())
                        .metadata(metadata.isEmpty() ? null : metadata)
                        .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                        .build();

                eventProducer.publishRestaurantEvent(restaurantCreatedEvent);
                log.debug("Published USER_CREATED event: userId={}, hasInterests={}, metadata={}",
                        restaurant.getRestaurantId(), metadata.containsKey("interests"), metadata);
            } catch (Exception e) {
                // Kafka publishing failures are non-critical - don't fail registration
                log.error("Error publishing USER_CREATED event during registration (non-critical, continuing registration): userId={}, error={}",
                        restaurant.getRestaurantId(), e.getMessage(), e);
                // Don't fail registration if event publishing fails - event publishing is non-critical
            }
        } else {
            log.warn("Kafka EventProducer not available - USER_CREATED event will not be published: userId={}",
                    restaurant.getRestaurantId());
        }

        RestaurantResponse response = restaurantMapper.toResponse(restaurant);
        return response;
    }

    /**
     * Admin login.
     */
    @Transactional
    public RestaurantLoginResponse login(String username, String password, String ipAddress) {
        Restaurant admin = restaurantRepository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> {
                    auditService.logLoginFailed(username, ipAddress, "User not found");
                    return new BusinessException("Invalid credentials");
                });

        if (admin.getIsBlocked()) {
            auditService.logLoginFailed(username, ipAddress, "Account locked");
            throw new BusinessException("Account is locked. Please try again later.");
        }

        if (!passwordEncoder.matches(password, admin.getPasswordHash())) {
            handleFailedLogin(admin, ipAddress);
            throw new BusinessException("Invalid credentials");
        }

        admin.setFailedLoginAttempts(0);
        admin.setLockedUntil(null);
        admin.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        restaurantRepository.save(admin);

        String jti = jwtTokenUtil.generateJti();
        String token = jwtTokenUtil.generateToken(
                admin.getRestaurantId(),
                admin.getName(),
                jti
        );

        auditService.logLogin(admin.getRestaurantId(), admin.getName(), ipAddress);

        log.info("Admin login successful: restaurantId={}, username={}", admin.getRestaurantId(), admin.getName());

        return RestaurantLoginResponse.builder()
                .token(token)
                .restaurantId(admin.getRestaurantId())
                .name(admin.getName())
                .build();
    }

    /**
     * Handles failed login attempt.
     */
    private void handleFailedLogin(Restaurant admin, String ipAddress) {
        int attempts = admin.getFailedLoginAttempts() + 1;
        admin.setFailedLoginAttempts(attempts);

        if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            admin.setLockedUntil(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(LOCKOUT_DURATION_MINUTES));
            log.warn("Admin account locked due to failed login attempts: restaurantId={}, username={}",
                    admin.getRestaurantId(), admin.getName());
        }

        restaurantRepository.save(admin);
        auditService.logLoginFailed(admin.getName(), ipAddress, "Failed attempt " + attempts + "/" + MAX_FAILED_LOGIN_ATTEMPTS);
    }

    /**
     * Admin logout
     */
    @Transactional
    public void logout(String token, Long restaurantId) {
        String jti = jwtTokenUtil.getJtiFromToken(token);

        // Log logout
        auditService.logLogout(restaurantId);

        log.info("Admin logout: restaurantId={}", restaurantId);
    }

    /**
     * Validate admin token
     */
    public boolean validateToken(String token) {
        try {
            // Extract JTI
            String jti = jwtTokenUtil.getJtiFromToken(token);
            if (jti == null) {
                return false;
            }

            // Validate ISS and AUD (MANDATORY)
            if (!jwtTokenUtil.validateIssuer(token)) {
                log.debug("Admin token ISS validation failed");
                return false;
            }

            if (!jwtTokenUtil.validateAudience(token)) {
                log.debug("Admin token AUD validation failed");
                return false;
            }

            // Validate signature and expiration
            return jwtTokenUtil.validateTokenSignature(token);

        } catch (Exception e) {
            log.debug("Admin token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get expiration time (for whitelist TTL)
     */
    public Long getExpiration() {
        return jwtTokenUtil.getExpiration();
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
}