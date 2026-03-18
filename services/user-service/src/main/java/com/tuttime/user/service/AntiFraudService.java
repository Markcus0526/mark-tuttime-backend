package com.tuttime.user.service;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.user.exception.UserServiceMessageKeys;
import com.tuttime.user.entity.UserDevice;
import com.tuttime.user.repository.UserDeviceRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.TransactionException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Anti-Fraud Service: tracks device usage, detects multi-account attempts, validates device registrations. Rules: unique device per user, max 3 devices, IP/device risk detection. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AntiFraudService {

    private final UserDeviceRepository userDeviceRepository;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Value("${antifraud.max-devices-per-user:3}")
    private int maxDevicesPerUser;

    @Value("${antifraud.multi-account-ip-threshold:3}")
    private int multiAccountIpThreshold;

    private static final String IP_ACCOUNTS_PREFIX = "fraud:ip:";
    private static final String IP_ACCOUNTS_SUFFIX = ":accounts";
    private static final String IP_DEVICE_COMBO_PREFIX = "fraud:ip:device:";
    private static final String IP_DEVICE_COMBO_SEPARATOR = ":";
    private static final String IP_DEVICE_COMBO_SUFFIX = ":accounts";
    private static final String DEVICE_USERS_PREFIX = "fraud:device:";
    private static final String DEVICE_USERS_SUFFIX = ":users";

    /**
     * Checks if a device is already registered to any user (device-only check, no IP).
     * Used during SMS signup to prevent sending SMS to devices already registered.
     * 
     * @param clientDeviceId Client device ID to check
     * @return true if device is registered to any user, false otherwise
     */
    public boolean isDeviceRegistered(String clientDeviceId) {
        if (clientDeviceId == null || clientDeviceId.trim().isEmpty()) {
            return false;
        }
        return userDeviceRepository.findByClientDeviceId(clientDeviceId).isPresent();
    }

    /** Validates device registration: checks device ID uniqueness, device limit, multi-account risk. */
    public void validateDeviceRegistration(String clientDeviceId, String ip, Long userId) {
        // Device ID is required
        if (clientDeviceId == null || clientDeviceId.trim().isEmpty()) {
            throw new BusinessException(
                    "Device ID is required",
                    UserServiceMessageKeys.DEVICE_ID_REQUIRED);
        }

        log.info("Device validation started: clientDeviceId={}, userId={}, ip={}", 
                clientDeviceId, userId, ip);
        
        // Check if client device ID is already used by another user
        boolean existsForOtherUser = userDeviceRepository.existsByClientDeviceIdAndUserIdNot(clientDeviceId, userId);
        log.info("Client device ID uniqueness check: clientDeviceId={}, existsForOtherUser={}, userId={}", 
                clientDeviceId, existsForOtherUser, userId);
        
        if (existsForOtherUser) {
            log.warn("Device registration blocked: Client device ID {} is already used by another user. userId={}, ip={}", 
                    clientDeviceId, userId, ip);
            throw new BusinessException(
                    "This device is already registered to another account. Please contact support.",
                    UserServiceMessageKeys.DEVICE_ALREADY_REGISTERED);
        }

        // Check device limit per user (skip for new user registration where userId is 0L)
        boolean isSignup = (userId == null || userId == 0L);
        if (!isSignup) {
            long deviceCount = userDeviceRepository.countByUserId(userId);
            if (deviceCount >= maxDevicesPerUser) {
                log.warn("Device registration blocked: User {} has reached maximum device limit ({}). deviceCount={}", 
                        userId, maxDevicesPerUser, deviceCount);
                throw new BusinessException(
                        String.format("Maximum device limit reached (%d devices). Please remove an existing device first.", 
                                maxDevicesPerUser),
                        UserServiceMessageKeys.DEVICE_LIMIT_REACHED);
            }
        }

        // Check multi-account risk
        checkMultiAccountRisk(clientDeviceId, ip, null);

        log.info("Device registration validated successfully: clientDeviceId={}, userId={}, ip={}, isSignup={}", 
                clientDeviceId, userId, ip, isSignup);
    }

    /**
     * Checks for multi-account fraud risk.
     * 
     * <p>Detects:
     * <ul>
     *   <li>Same IP + Same Device ID with multiple accounts (fraud indicator)</li>
     *   <li>Same device ID with different users (handled by database check)</li>
     * </ul>
     * 
     * <p>Note: IP+Device combination check allows legitimate users on shared networks
     * (home networks, offices, public WiFi) while still detecting fraud when the same
     * device is used to create multiple accounts from the same IP.
     *
     * @param deviceId Device ID to check
     * @param ip IP address to check
     * @param phoneNumber Phone number to check (optional)
     * @throws BusinessException if multi-account risk is detected
     */
    public void checkMultiAccountRisk(String deviceId, String ip, String phoneNumber) {
        if (redisTemplate == null) {
            log.debug("Redis not available - multi-account risk check skipped");
            return;
        }

        // Check IP+Device combination multi-account risk (device-aware)
        // Only block if same IP + same Device ID has multiple accounts (fraud indicator)
        // This allows legitimate users on shared networks (same IP, different devices)
        if (ip != null && !ip.trim().isEmpty() && deviceId != null && !deviceId.trim().isEmpty()) {
            String ipDeviceKey = buildIpDeviceComboKey(ip, deviceId);
            Long accountCount = redisTemplate.opsForSet().size(ipDeviceKey);
            
            if (accountCount != null && accountCount >= multiAccountIpThreshold) {
                log.warn("Multi-account risk detected: IP {} + Device {} combination has {} accounts registered in last 24h (threshold: {})", 
                        ip, deviceId, accountCount, multiAccountIpThreshold);
                throw new BusinessException(
                        "Multiple account registration detected from this IP address and device. Please contact support.",
                        UserServiceMessageKeys.MULTI_ACCOUNT_DETECTED_IP);
            } else if (accountCount != null && accountCount > 0) {
                log.debug("IP {} + Device {} combination has {} account(s) registered in last 24h (threshold: {})", 
                        ip, deviceId, accountCount, multiAccountIpThreshold);
            }
        } else if (ip != null && !ip.trim().isEmpty()) {
            // Fallback: If device ID is not provided, check IP alone (legacy support)
            // This is less strict but allows registration to proceed
            String ipKey = IP_ACCOUNTS_PREFIX + ip + IP_ACCOUNTS_SUFFIX;
            Long accountCount = redisTemplate.opsForSet().size(ipKey);
            
            if (accountCount != null && accountCount > 0) {
                log.debug("IP {} has {} account(s) registered in last 24h (device ID not provided, using IP-only check)", 
                        ip, accountCount);
                // Note: We don't block here if device ID is missing - just log for monitoring
                // The device uniqueness check in validateDeviceRegistration will handle device-based blocking
            }
        }

        // Check device-based multi-account risk (for monitoring only)
        // Note: This only checks Redis cache, not database, so it's a soft check
        // The database check in validateDeviceRegistration is more reliable
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            String deviceKey = DEVICE_USERS_PREFIX + deviceId + DEVICE_USERS_SUFFIX;
            Long userCount = redisTemplate.opsForSet().size(deviceKey);
            
            if (userCount != null && userCount > 1) {
                log.warn("Multi-account risk detected: Device {} is used by {} users (Redis cache)", deviceId, userCount);
                // Note: We don't throw here during signup as the database check is more reliable
                // This is just a warning for monitoring
            }
        }
    }

    /**
     * Builds a Redis key for IP+Device combination tracking.
     * 
     * @param ip IP address
     * @param deviceId Device ID
     * @return Redis key string
     */
    private String buildIpDeviceComboKey(String ip, String deviceId) {
        // Format: fraud:ip:device:{ip}:{deviceId}:accounts
        // We sanitize the deviceId to avoid issues with special characters in Redis keys
        String sanitizedDeviceId = deviceId.trim().replaceAll("[^a-zA-Z0-9\\-]", "_");
        return IP_DEVICE_COMBO_PREFIX + ip.trim() + IP_DEVICE_COMBO_SEPARATOR + sanitizedDeviceId + IP_DEVICE_COMBO_SUFFIX;
    }

    /**
     * Tracks device usage for a user.
     * 
     * <p>Creates or updates a device record and tracks usage in Redis for fraud detection.
     * 
     * <p><b>Note:</b> This method uses REQUIRES_NEW propagation to ensure device tracking
     * failures don't affect the main login transaction. Device tracking is non-critical
     * and should not block login.
     * 
     * <p><b>Transaction Management:</b>
     * Since all exceptions are caught and not rethrown, the transaction will commit
     * successfully even if errors occur. This ensures the connection is properly released
     * and prevents connection leaks.
     *
     * @param clientDeviceId Client-provided device ID (UUID from mobile app) - required
     * @param userId User ID
     * @param ip IP address
     */
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW,
                   timeout = 60,
                   noRollbackFor = {Exception.class})
    public void trackDeviceUsage(String clientDeviceId, Long userId, String ip) {
        if (clientDeviceId == null || clientDeviceId.trim().isEmpty()) {
            log.debug("Client device ID not provided - skipping device tracking");
            return;
        }

        log.debug("Starting device usage tracking: clientDeviceId={}, userId={}, ip={}", clientDeviceId, userId, ip);
        
        try {
            HttpServletRequest request = getHttpServletRequest();

            // Try to find device by client device ID
            log.debug("Looking up device: userId={}, clientDeviceId={}", userId, clientDeviceId);
            
            // First check if device exists for this user
            UserDevice device = userDeviceRepository.findByUserIdAndClientDeviceId(userId, clientDeviceId)
                    .orElse(null);
            
            // If not found, check if device exists globally (might be a transaction isolation issue)
            // This handles cases where the device was created in a previous transaction
            if (device == null) {
                Optional<UserDevice> globalDevice = userDeviceRepository.findByClientDeviceId(clientDeviceId);
                if (globalDevice.isPresent()) {
                    UserDevice foundDevice = globalDevice.get();
                    if (foundDevice.getUserId().equals(userId)) {
                        // Device exists for this user - use it (transaction isolation issue)
                        device = foundDevice;
                        log.debug("Found device globally for same user (transaction isolation): deviceId={}, userId={}", 
                                device.getDeviceId(), userId);
                    } else {
                        // Device exists for different user - this is a multi-account scenario
                        // Log warning but don't fail - device tracking is non-critical
                        log.warn("Device clientDeviceId={} already exists for different user (userId={}, existingUserId={}). " +
                                "This may indicate multi-account usage. Skipping device creation.", 
                                clientDeviceId, userId, foundDevice.getUserId());
                        // Don't create device - unique constraint would fail anyway
                        return;
                    }
                }
            }
            log.debug("Device lookup completed: deviceFound={}", device != null);

            if (device == null) {
                // Generate a placeholder fingerprint for database compatibility (not used for validation)
                String placeholderFingerprint = "placeholder-" + clientDeviceId;
                
                // Check if device already exists by fingerprint (to avoid unique constraint violation)
                // This handles cases where transaction isolation prevents seeing the device
                Optional<UserDevice> existingByFingerprint = userDeviceRepository
                        .findByUserIdAndDeviceFingerprint(userId, placeholderFingerprint);
                
                if (existingByFingerprint.isPresent()) {
                    // Device exists by fingerprint - use it (transaction isolation issue)
                    device = existingByFingerprint.get();
                    log.debug("Found device by fingerprint (transaction isolation): deviceId={}, userId={}, fingerprint={}", 
                            device.getDeviceId(), userId, placeholderFingerprint);
                } else {
                    // Create new device record
                    // Extract device type - prefer custom header, fallback to User-Agent parsing
                    String deviceTypeStr = request != null ? request.getHeader("X-Device-Type") : null;
                    if (deviceTypeStr == null || deviceTypeStr.trim().isEmpty()) {
                        deviceTypeStr = extractDeviceType(request);
                    }
                    UserDevice.DeviceType deviceTypeEnum = mapDeviceType(deviceTypeStr);
                    
                    String userAgent = request != null ? request.getHeader("User-Agent") : null;
                    
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

                    device = UserDevice.builder()
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

                    try {
                        userDeviceRepository.save(device);
                        log.info("New device registered: clientDeviceId={}, deviceType={}, deviceModel={}, osVersion={}, appVersion={}, userId={}, ip={}", 
                                clientDeviceId, deviceTypeEnum, deviceModel, osVersion, appVersion, userId, ip);
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // Unique constraint violation - device might have been created concurrently
                        // Try to find it again
                        log.warn("Unique constraint violation during device save (concurrent insert?), retrying lookup: clientDeviceId={}, userId={}, error={}", 
                                clientDeviceId, userId, e.getMessage());
                        device = userDeviceRepository.findByUserIdAndClientDeviceId(userId, clientDeviceId)
                                .orElse(userDeviceRepository.findByUserIdAndDeviceFingerprint(userId, placeholderFingerprint)
                                        .orElse(null));
                        if (device != null) {
                            log.debug("Found device after constraint violation: deviceId={}, userId={}", device.getDeviceId(), userId);
                        } else {
                            // Re-throw if we still can't find it
                            throw e;
                        }
                    }
                }
            } else {
                // Update existing device
                device.setLastSeen(OffsetDateTime.now(ZoneOffset.UTC));
                if (ip != null && !ip.equals(device.getIpAddress())) {
                    device.setIpAddress(ip);
                }
                // Update client device ID if it was missing
                if (device.getClientDeviceId() == null || device.getClientDeviceId().trim().isEmpty()) {
                    device.setClientDeviceId(clientDeviceId);
                }
                userDeviceRepository.save(device);
                log.debug("Device usage tracked: clientDeviceId={}, userId={}", clientDeviceId, userId);
            }

            // Track in Redis for fraud detection (use client device ID)
            trackInRedis(clientDeviceId, userId, ip);
            
            log.debug("Device usage tracking completed successfully: clientDeviceId={}, userId={}", clientDeviceId, userId);

        } catch (org.springframework.transaction.UnexpectedRollbackException e) {
            // Unexpected rollback - transaction was marked for rollback (e.g., due to constraint violation)
            // This is non-critical - log and continue without blocking login
            log.error("Transaction unexpectedly rolled back in device tracking (non-critical, continuing login): clientDeviceId={}, userId={}, error={}", 
                    clientDeviceId, userId, e.getMessage(), e);
            // Don't rethrow - device tracking is non-critical
        } catch (org.springframework.dao.QueryTimeoutException e) {
            // Query timeout - insert took too long (likely due to index lock)
            // This is non-critical - log and continue without blocking login
            log.error("Query timeout during device tracking (non-critical, continuing login): clientDeviceId={}, userId={}, error={}", 
                    clientDeviceId, userId, e.getMessage(), e);
            // Don't rethrow - device tracking is non-critical
        } catch (org.hibernate.QueryTimeoutException e) {
            // Hibernate query timeout - insert took too long
            // This is non-critical - log and continue without blocking login
            String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
            log.error("Hibernate query timeout during device tracking (non-critical, continuing login): clientDeviceId={}, userId={}, error={}, stackTrace={}", 
                    clientDeviceId, userId, e.getMessage(), stackTraceSummary);
            // Don't rethrow - device tracking is non-critical
        } catch (org.springframework.dao.DataAccessException e) {
            // Database exceptions - log but don't rethrow (device tracking is non-critical)
            // Using REQUIRES_NEW ensures this doesn't affect the main transaction
            String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
            log.error("Database error tracking device usage (non-critical, continuing login): clientDeviceId={}, userId={}, error={}, stackTrace={}", 
                    clientDeviceId, userId, e.getMessage(), stackTraceSummary);
            // Transaction will rollback, but main login transaction continues
        } catch (org.springframework.transaction.TransactionException e) {
            // Transaction exceptions - check if this is a rollback-only or timeout error
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown transaction error";
            if (errorMsg.contains("rollback-only")) {
                log.error("Transaction rollback-only error in device tracking (non-critical, continuing login): clientDeviceId={}, userId={}, error={}", 
                        clientDeviceId, userId, errorMsg, e);
            } else if (errorMsg.contains("timeout") || errorMsg.contains("Timeout")) {
                log.error("Transaction timeout in device tracking (non-critical, continuing login): clientDeviceId={}, userId={}, error={}", 
                        clientDeviceId, userId, errorMsg, e);
            } else {
                log.error("Transaction exception tracking device usage (non-critical, continuing login): clientDeviceId={}, userId={}, error={}", 
                        clientDeviceId, userId, errorMsg, e);
            }
            // Don't rethrow - device tracking is non-critical
        } catch (Exception e) {
            // Other exceptions - log but don't rethrow (device tracking is non-critical)
            log.error("Error tracking device usage (non-critical, continuing login): clientDeviceId={}, userId={}, errorType={}, error={}", 
                    clientDeviceId, userId, e.getClass().getSimpleName(), e.getMessage(), e);
            // Don't throw exception - device tracking failure shouldn't block login
        } finally {
            log.debug("Device usage tracking method completed: clientDeviceId={}, userId={}", clientDeviceId, userId);
        }
    }

    /**
     * Generates a device fingerprint from request characteristics.
     * 
     * <p>Creates a hash from various request headers and characteristics to uniquely
     * identify a device, helping detect device spoofing.
     * 
     * <p>Includes client device ID if available to ensure uniqueness across devices
     * on the same network (which may share IP and similar headers).
     *
     * @param request HTTP request
     * @return Device fingerprint hash
     */
    public String getDeviceFingerprint(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        try {
            StringBuilder fingerprint = new StringBuilder();
            
            // Include client device ID if available (most unique identifier - CRITICAL for distinguishing devices)
            String clientDeviceId = request.getHeader("X-Device-Id");
            if (clientDeviceId != null && !clientDeviceId.trim().isEmpty()) {
                fingerprint.append("DEVICE_ID:").append(clientDeviceId.trim()).append("|");
            }
            
            // Include device-specific headers (CRITICAL for distinguishing devices of same type)
            // These headers make fingerprints unique even for same device type on same network
            String deviceType = request.getHeader("X-Device-Type");
            if (deviceType != null && !deviceType.trim().isEmpty()) {
                fingerprint.append("DEVICE_TYPE:").append(deviceType.trim()).append("|");
            }
            
            String deviceModel = request.getHeader("X-Device-Model");
            if (deviceModel != null && !deviceModel.trim().isEmpty()) {
                fingerprint.append("DEVICE_MODEL:").append(deviceModel.trim()).append("|");
            }
            
            String osVersion = request.getHeader("X-OS-Version");
            if (osVersion != null && !osVersion.trim().isEmpty()) {
                fingerprint.append("OS_VERSION:").append(osVersion.trim()).append("|");
            }
            
            String appVersion = request.getHeader("X-App-Version");
            if (appVersion != null && !appVersion.trim().isEmpty()) {
                fingerprint.append("APP_VERSION:").append(appVersion.trim()).append("|");
            }
            
            // Combine various request characteristics (less unique but still useful)
            String userAgent = request.getHeader("User-Agent");
            if (userAgent != null) {
                fingerprint.append("UA:").append(userAgent).append("|");
            }
            
            String acceptLanguage = request.getHeader("Accept-Language");
            if (acceptLanguage != null) {
                fingerprint.append("LANG:").append(acceptLanguage).append("|");
            }
            
            String acceptEncoding = request.getHeader("Accept-Encoding");
            if (acceptEncoding != null) {
                fingerprint.append("ENC:").append(acceptEncoding).append("|");
            }
            
            // Get IP address (prefer X-Forwarded-For or X-Real-IP since we're behind proxy)
            String ip = extractIpAddress(request);
            if (ip != null && !ip.trim().isEmpty()) {
                fingerprint.append("IP:").append(ip.trim()).append("|");
            }
            
            // If no client device ID and no device-specific headers, log warning
            if ((clientDeviceId == null || clientDeviceId.trim().isEmpty()) && 
                (deviceModel == null || deviceModel.trim().isEmpty())) {
                log.warn("Device fingerprint generated without client device ID or device model - may not be unique: " +
                        "deviceType={}, osVersion={}, appVersion={}, ip={}", 
                        deviceType, osVersion, appVersion, ip);
            }
            
            String fingerprintString = fingerprint.toString();
            log.debug("Device fingerprint components: {}", fingerprintString);
            
            // Create SHA-256 hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprintString.getBytes(StandardCharsets.UTF_8));
            
            // Convert to hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            String fingerprintHash = hexString.toString();
            log.debug("Generated device fingerprint hash: {} (length: {})", fingerprintHash, fingerprintHash.length());
            return fingerprintHash;
        } catch (Exception e) {
            log.warn("Error generating device fingerprint: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Extracts IP address from request, checking X-Forwarded-For, X-Real-IP, and remote address.
     */
    private String extractIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        // Check X-Forwarded-For header (first IP in chain)
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.trim().isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            String ip = ips[0].trim();
            if (!ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip;
            }
        }
        
        // Check X-Real-IP header
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.trim().isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp.trim();
        }
        
        // Fallback to remote address
        return request.getRemoteAddr();
    }

    /**
     * Tracks device and IP usage in Redis for fraud detection.
     */
    private void trackInRedis(String deviceId, Long userId, String ip) {
        if (redisTemplate == null) {
            return;
        }

        try {
            // Track IP-based account registration (24 hour TTL) - for legacy support
            if (ip != null && !ip.trim().isEmpty()) {
                String ipKey = IP_ACCOUNTS_PREFIX + ip + IP_ACCOUNTS_SUFFIX;
                redisTemplate.opsForSet().add(ipKey, userId.toString());
                redisTemplate.expire(ipKey, 24, TimeUnit.HOURS);
            }

            // Track IP+Device combination for device-aware fraud detection (24 hour TTL)
            // This is the primary fraud detection mechanism
            if (ip != null && !ip.trim().isEmpty() && deviceId != null && !deviceId.trim().isEmpty()) {
                String ipDeviceKey = buildIpDeviceComboKey(ip, deviceId);
                redisTemplate.opsForSet().add(ipDeviceKey, userId.toString());
                redisTemplate.expire(ipDeviceKey, 24, TimeUnit.HOURS);
                log.debug("Tracked IP+Device combination in Redis: ip={}, deviceId={}, userId={}", ip, deviceId, userId);
            }

            // Track device-based user association (30 day TTL)
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                String deviceKey = DEVICE_USERS_PREFIX + deviceId + DEVICE_USERS_SUFFIX;
                redisTemplate.opsForSet().add(deviceKey, userId.toString());
                redisTemplate.expire(deviceKey, 30, TimeUnit.DAYS);
            }
        } catch (Exception e) {
            log.warn("Error tracking in Redis: deviceId={}, userId={}, ip={}", deviceId, userId, ip, e);
        }
    }

    /**
     * Clears anti-fraud Redis cache for a specific IP address.
     * Useful for development/testing when you need to reset IP-based restrictions.
     *
     * @param ip IP address to clear (if null, clears all IP-based fraud data)
     */
    public void clearIpFraudCache(String ip) {
        if (redisTemplate == null) {
            log.warn("Redis not available - cannot clear IP fraud cache");
            return;
        }

        try {
            if (ip != null && !ip.trim().isEmpty()) {
                // Clear specific IP (legacy key)
                String ipKey = IP_ACCOUNTS_PREFIX + ip + IP_ACCOUNTS_SUFFIX;
                redisTemplate.delete(ipKey);
                
                // Clear all IP+Device combination keys for this IP
                String ipDevicePattern = IP_DEVICE_COMBO_PREFIX + ip + "*" + IP_DEVICE_COMBO_SUFFIX;
                Set<String> ipDeviceKeys = redisTemplate.keys(ipDevicePattern);
                if (ipDeviceKeys != null && !ipDeviceKeys.isEmpty()) {
                    redisTemplate.delete(ipDeviceKeys);
                    log.info("Cleared fraud cache for IP: {} (including {} IP+Device combination entries)", ip, ipDeviceKeys.size());
                } else {
                    log.info("Cleared fraud cache for IP: {}", ip);
                }
            } else {
                // Clear all IP-based fraud keys (legacy)
                String pattern = IP_ACCOUNTS_PREFIX + "*" + IP_ACCOUNTS_SUFFIX;
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("Cleared {} IP-based fraud cache entries (legacy)", keys.size());
                }
                
                // Clear all IP+Device combination keys
                String ipDevicePattern = IP_DEVICE_COMBO_PREFIX + "*" + IP_DEVICE_COMBO_SUFFIX;
                Set<String> ipDeviceKeys = redisTemplate.keys(ipDevicePattern);
                if (ipDeviceKeys != null && !ipDeviceKeys.isEmpty()) {
                    redisTemplate.delete(ipDeviceKeys);
                    log.info("Cleared {} IP+Device combination fraud cache entries", ipDeviceKeys.size());
                }
            }
        } catch (Exception e) {
            log.error("Error clearing IP fraud cache: ip={}", ip, e);
            throw new BusinessException("Failed to clear IP fraud cache: " + e.getMessage());
        }
    }

    /**
     * Clears anti-fraud Redis cache for a specific device.
     * Useful for development/testing when you need to reset device-based restrictions.
     *
     * @param deviceId Device ID to clear (if null, clears all device-based fraud data)
     */
    public void clearDeviceFraudCache(String deviceId) {
        if (redisTemplate == null) {
            log.warn("Redis not available - cannot clear device fraud cache");
            return;
        }

        try {
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                // Clear specific device
                String deviceKey = DEVICE_USERS_PREFIX + deviceId + DEVICE_USERS_SUFFIX;
                redisTemplate.delete(deviceKey);
                log.info("Cleared fraud cache for device: {}", deviceId);
            } else {
                // Clear all device-based fraud keys
                String pattern = DEVICE_USERS_PREFIX + "*" + DEVICE_USERS_SUFFIX;
                Set<String> keys = redisTemplate.keys(pattern);
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                    log.info("Cleared {} device-based fraud cache entries", keys.size());
                }
            }
        } catch (Exception e) {
            log.error("Error clearing device fraud cache: deviceId={}", deviceId, e);
            throw new BusinessException("Failed to clear device fraud cache: " + e.getMessage());
        }
    }

    /**
     * Clears all anti-fraud Redis cache (both IP and device based).
     * Use with caution - this clears all fraud detection data.
     */
    public void clearAllFraudCache() {
        if (redisTemplate == null) {
            log.warn("Redis not available - cannot clear fraud cache");
            return;
        }

        try {
            // Clear all IP-based fraud keys (legacy)
            String ipPattern = IP_ACCOUNTS_PREFIX + "*" + IP_ACCOUNTS_SUFFIX;
            Set<String> ipKeys = redisTemplate.keys(ipPattern);
            if (ipKeys != null && !ipKeys.isEmpty()) {
                redisTemplate.delete(ipKeys);
                log.info("Cleared {} IP-based fraud cache entries (legacy)", ipKeys.size());
            }

            // Clear all IP+Device combination keys
            String ipDevicePattern = IP_DEVICE_COMBO_PREFIX + "*" + IP_DEVICE_COMBO_SUFFIX;
            Set<String> ipDeviceKeys = redisTemplate.keys(ipDevicePattern);
            if (ipDeviceKeys != null && !ipDeviceKeys.isEmpty()) {
                redisTemplate.delete(ipDeviceKeys);
                log.info("Cleared {} IP+Device combination fraud cache entries", ipDeviceKeys.size());
            }

            // Clear all device-based fraud keys
            String devicePattern = DEVICE_USERS_PREFIX + "*" + DEVICE_USERS_SUFFIX;
            Set<String> deviceKeys = redisTemplate.keys(devicePattern);
            if (deviceKeys != null && !deviceKeys.isEmpty()) {
                redisTemplate.delete(deviceKeys);
                log.info("Cleared {} device-based fraud cache entries", deviceKeys.size());
            }

            log.info("All fraud cache cleared successfully");
        } catch (Exception e) {
            log.error("Error clearing all fraud cache", e);
            throw new BusinessException("Failed to clear fraud cache: " + e.getMessage());
        }
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
            // Try to extract iPhone model
            if (ua.contains("iphone15")) return "iPhone 15";
            if (ua.contains("iphone14")) return "iPhone 14";
            if (ua.contains("iphone13")) return "iPhone 13";
            if (ua.contains("iphone12")) return "iPhone 12";
            return "iPhone";
        } else if (ua.contains("ipad")) {
            return "iPad";
        } else if (ua.contains("android")) {
            // Try to extract Android device model
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

        // Simple extraction - can be enhanced with more sophisticated parsing
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
}

