package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.user.dto.PasswordResetInitiationResult;
import com.tuttime.user.entity.User;
import com.tuttime.user.exception.UserServiceMessageKeys;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.service.EmailVerificationService;
import com.tuttime.user.service.PasswordResetService;
import com.tuttime.user.service.SmsVerificationService;
import com.tuttime.user.service.TokenWhitelistService;
import com.tuttime.user.util.JwtTokenUtil;
import com.tuttime.user.util.PhoneNumberUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of Password Reset Service
 * 
 * <p>Handles password reset flow with retry-aware channel escalation:
 * <ol>
 *   <li>1st request: try Email</li>
 *   <li>2nd request (resend): try Telegram</li>
 *   <li>3rd+ request (resend): try SMS</li>
 *   <li>Verify code via the channel used for this request</li>
 *   <li>Reset password using reset token</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final EmailVerificationService emailVerificationService;
    private final SmsVerificationService smsVerificationService;
    private final JwtTokenUtil jwtTokenUtil;
    private final PasswordEncoder passwordEncoder;
    private final TokenWhitelistService tokenWhitelistService;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Value("${email.code.expiration-minutes:10}")
    private int emailCodeExpirationMinutes;

    @Value("${sms.signup.token.expiration-minutes:15}")
    private int resetTokenExpirationMinutes;

    /** Redis key prefix for reset tokens */
    private static final String RESET_TOKEN_PREFIX = "password-reset:token:";
    
    /** Redis key prefix for retry state */
    private static final String RESET_RETRY_PREFIX = "password-reset:retry:";
    
    /** Redis key prefix for channel used (for verify step) */
    private static final String RESET_CHANNEL_PREFIX = "password-reset:channel:";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public PasswordResetInitiationResult initiatePasswordReset(String phoneNumber, String deviceType, Boolean hasTelegram) {
        log.info("Initiating password reset for phone: {}, deviceType: {}, hasTelegram: {}", 
                phoneNumber, deviceType, hasTelegram);

        // Validate phone number format
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BusinessException("Phone number is required");
        }

        // Normalize phone number before lookup for consistent comparison
        String normalizedPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);

        // Find user by phone number (but don't reveal if they don't exist)
        Optional<User> userOpt = userRepository.findByPhone(normalizedPhone);

        if (userOpt.isEmpty()) {
            log.debug("Password reset requested for non-existent phone: {}", phoneNumber);
            return PasswordResetInitiationResult.builder()
                    .verificationMethod("Email")
                    .sentTo("email")
                    .build();
        }

        User user = userOpt.get();

        // Build ordered list of available methods: Email -> Telegram -> SMS
        List<String> availableMethods = buildAvailableMethods(user, true);
        if (availableMethods.isEmpty()) {
            log.warn("No verification methods available for user: {}", user.getUserId());
            throw new BusinessException(
                    "No verification method available. Please contact support.",
                    UserServiceMessageKeys.PASSWORD_RESET_VERIFICATION_SEND_FAILED);
        }

        // Read retry state from Redis
        RetryState retryState = getRetryState(normalizedPhone);
        int retryCount = retryState.count();
        int methodIndex = Math.min(retryCount, availableMethods.size() - 1);
        String targetMethod = availableMethods.get(methodIndex);

        // Try to send via target method, fall back to next on failure
        String verificationMethod = null;
        String sentTo = null;
        int methodsTried = 0;

        for (int i = methodIndex; i < availableMethods.size(); i++) {
            String method = availableMethods.get(i);
            methodsTried++;
            try {
                SendResult result = trySendViaMethod(method, user, phoneNumber, deviceType, true);
                if (result != null) {
                    verificationMethod = result.verificationMethod();
                    sentTo = result.sentTo();
                    break;
                }
            } catch (Exception e) {
                log.warn("Failed to send via {} for phone {}: {}, trying next method", 
                        method, phoneNumber, e.getMessage());
            }
        }

        if (verificationMethod == null) {
            log.error("All verification methods failed for phone: {}", phoneNumber);
            throw new BusinessException(
                    "Failed to send verification code. Please try again later.",
                    UserServiceMessageKeys.PASSWORD_RESET_VERIFICATION_SEND_FAILED);
        }

        // Save retry state and channel for verify step
        saveRetryState(normalizedPhone, retryCount + 1, verificationMethod);
        storeResetChannel(normalizedPhone, verificationMethod);

        int resultRetryCount = retryCount + 1;
        log.info("Password reset code sent via {} to {} (phone: {}, attempt {})", 
                verificationMethod, sentTo, phoneNumber, resultRetryCount);

        return PasswordResetInitiationResult.builder()
                .verificationMethod(verificationMethod)
                .sentTo(sentTo)
                .retryCount(resultRetryCount)
                .build();
    }

    /**
     * Builds ordered list of available verification methods for the user.
     * Email is tried first whenever the user has an email address, regardless of email verification status.
     */
    private List<String> buildAvailableMethods(User user, boolean hasTelegram) {
        List<String> methods = new ArrayList<>();
        // Try Email first if user has email - even if not yet verified
        if (user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            methods.add("Email");
        }
        if (hasTelegram) {
            methods.add("Telegram");
        }
        methods.add("SMS"); // Always available as final fallback
        return methods;
    }

    /**
     * Attempts to send verification code via the specified method.
     */
    private SendResult trySendViaMethod(String method, User user, String phoneNumber, String deviceType, boolean hasTelegram) {
        switch (method) {
            case "Email":
                emailVerificationService.sendVerificationCode(user.getEmail(), null);
                return new SendResult("Email", user.getEmail());
            case "Telegram":
                String smsMethod = smsVerificationService.sendSignupSms(phoneNumber, deviceType, true);
                return new SendResult(smsMethod, determineSentTo(smsMethod));
            case "SMS":
                String smsMethod2 = smsVerificationService.sendSignupSms(phoneNumber, deviceType, false);
                return new SendResult(smsMethod2, determineSentTo(smsMethod2));
            default:
                return null;
        }
    }

    private record SendResult(String verificationMethod, String sentTo) {}

    @SuppressWarnings("unchecked")
    private RetryState getRetryState(String normalizedPhone) {
        if (redisTemplate == null) {
            return new RetryState(0, null);
        }
        try {
            String key = RESET_RETRY_PREFIX + normalizedPhone;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.trim().isEmpty()) {
                return new RetryState(0, null);
            }
            var map = objectMapper.readValue(json, java.util.Map.class);
            int count = map.get("count") != null ? ((Number) map.get("count")).intValue() : 0;
            String lastMethod = map.get("lastMethod") != null ? map.get("lastMethod").toString() : null;
            return new RetryState(count, lastMethod);
        } catch (Exception e) {
            log.debug("Failed to parse retry state for {}: {}", normalizedPhone, e.getMessage());
            return new RetryState(0, null);
        }
    }

    private void saveRetryState(String normalizedPhone, int count, String lastMethod) {
        if (redisTemplate == null) return;
        try {
            String key = RESET_RETRY_PREFIX + normalizedPhone;
            String json = objectMapper.writeValueAsString(java.util.Map.of("count", count, "lastMethod", lastMethod));
            redisTemplate.opsForValue().set(key, json, resetTokenExpirationMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to save retry state for {}: {}", normalizedPhone, e.getMessage());
        }
    }

    private void clearRetryState(String normalizedPhone) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(RESET_RETRY_PREFIX + normalizedPhone);
            redisTemplate.delete(RESET_CHANNEL_PREFIX + normalizedPhone);
        } catch (Exception e) {
            log.warn("Failed to clear retry state for {}: {}", normalizedPhone, e.getMessage());
        }
    }

    private void storeResetChannel(String normalizedPhone, String channel) {
        if (redisTemplate == null) return;
        try {
            String key = RESET_CHANNEL_PREFIX + normalizedPhone;
            redisTemplate.opsForValue().set(key, channel, resetTokenExpirationMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Failed to store reset channel for {}: {}", normalizedPhone, e.getMessage());
        }
    }

    private String getStoredResetChannel(String normalizedPhone) {
        if (redisTemplate == null) return null;
        try {
            return redisTemplate.opsForValue().get(RESET_CHANNEL_PREFIX + normalizedPhone);
        } catch (Exception e) {
            return null;
        }
    }

    private record RetryState(int count, String lastMethod) {}
    
    /**
     * Determines the sentTo value based on the verification method.
     * 
     * @param verificationMethod The verification method used
     * @return User-friendly string indicating where the code was sent
     */
    private String determineSentTo(String verificationMethod) {
        if (verificationMethod == null) {
            return "SMS";
        }
        
        switch (verificationMethod) {
            case "Telegram Gateway":
                return "your Telegram account";
            case "Flash Call":
                return "flash call";
            case "Green SMS":
            case "Twilio":
            default:
                return "SMS";
        }
    }

    @Override
    @Transactional
    public String verifyCodeAndGenerateResetToken(String phoneNumber, String code) {
        log.info("Verifying code for password reset, phone: {}", phoneNumber);

        // Normalize phone number before lookup for consistent comparison
        String normalizedPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);

        // Find user by phone number
        User user = userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Use stored channel from initiate step (Email vs SMS/Telegram/Flash Call)
        // Fall back to user.getEmailVerified() if channel not stored (e.g. Redis cleared)
        String storedChannel = getStoredResetChannel(normalizedPhone);
        boolean useEmail = "Email".equals(storedChannel) || 
                (storedChannel == null && user.getEmailVerified() != null && user.getEmailVerified() 
                        && user.getEmail() != null && !user.getEmail().trim().isEmpty());

        if (useEmail && user.getEmail() != null && !user.getEmail().trim().isEmpty()) {
            if (!emailVerificationService.verifyCode(user.getEmail(), code)) {
                throw new BusinessException("Invalid or expired email code. Please try again.");
            }
            log.info("Email code verified for password reset, phone: {}", phoneNumber);
        } else {
            if (!smsVerificationService.verifyCode(phoneNumber, code)) {
                throw new BusinessException("Invalid or expired SMS code. Please try again.");
            }
            log.info("SMS code verified for password reset, phone: {}", phoneNumber);
        }

        // Clear retry state after successful verification
        clearRetryState(normalizedPhone);

        // Generate reset token (use normalized phone for consistency)
        String resetTokenJti = jwtTokenUtil.generateJti();
        String resetToken = jwtTokenUtil.generateResetToken(normalizedPhone, resetTokenJti);

        // Store reset token in Redis
        if (redisTemplate != null) {
            String tokenKey = RESET_TOKEN_PREFIX + resetTokenJti;
            redisTemplate.opsForValue().set(
                    tokenKey,
                    normalizedPhone,
                    resetTokenExpirationMinutes,
                    TimeUnit.MINUTES
            );
            log.debug("Reset token stored in Redis: jti={}, phone={}", resetTokenJti, normalizedPhone);
        } else {
            log.warn("Redis not available - reset token not stored. Token will not be revocable.");
        }

        log.info("Reset token generated for phone: {} (normalized: {})", phoneNumber, normalizedPhone);
        return resetToken;
    }

    @Override
    public String validateResetToken(String resetToken) {
        if (resetToken == null || resetToken.trim().isEmpty()) {
            throw new BusinessException("Reset token is required");
        }

        // Validate JWT token
        if (!jwtTokenUtil.validateToken(resetToken)) {
            throw new BusinessException("Invalid or expired reset token");
        }

        // Check token type
        String tokenType = jwtTokenUtil.getClaimFromToken(resetToken, claims -> claims.get("type", String.class));
        if (!"password-reset".equals(tokenType)) {
            throw new BusinessException("Invalid token type - password reset token required");
        }

        String jti = jwtTokenUtil.getJtiFromToken(resetToken);
        if (jti == null) {
            throw new BusinessException("Invalid reset token - missing JTI");
        }

        String tokenPhoneNumber = jwtTokenUtil.getPhoneNumberFromToken(resetToken);
        if (tokenPhoneNumber == null) {
            throw new BusinessException("Invalid reset token - missing phone number claim");
        }

        // Validate token in Redis
        if (redisTemplate == null) {
            log.warn("Redis not available - reset token validation skipped. Token will be considered valid if JWT is valid.");
            return tokenPhoneNumber; // Fail open
        }

        String tokenKey = RESET_TOKEN_PREFIX + jti;
        String storedPhoneNumber = redisTemplate.opsForValue().get(tokenKey);

        if (storedPhoneNumber == null) {
            throw new BusinessException("Reset token has expired or been used");
        }

        if (!tokenPhoneNumber.equals(storedPhoneNumber)) {
            throw new BusinessException("Invalid reset token - phone number mismatch");
        }

        return tokenPhoneNumber;
    }

    @Override
    @Transactional
    public void resetPassword(String resetToken, String newPassword) {
        log.info("Resetting password using reset token");

        // Validate reset token and get phone number (already normalized)
        String phoneNumber = validateResetToken(resetToken);
        
        // Ensure phone number is normalized (defense in depth)
        String normalizedPhone = PhoneNumberUtil.normalizePhoneNumber(phoneNumber);

        // Find user
        User user = userRepository.findByPhone(normalizedPhone)
                .orElseThrow(() -> new BusinessException("User not found"));

        // Validate password strength (basic validation - can be enhanced)
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BusinessException("New password is required");
        }
        if (newPassword.length() < 6) {
            throw new BusinessException("Password must be at least 6 characters");
        }

        // Check if new password is same as current password
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessException("New password must be different from current password");
        }

        // Hash and update password
        String hashedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        log.info("Password updated for user: userId={}, phone={}", user.getUserId(), normalizedPhone);

        // Invalidate reset token
        invalidateResetToken(resetToken);

        // Revoke all user tokens (force re-login)
        int revokedCount = tokenWhitelistService.revokeAllUserTokens(user.getUserId());
        log.info("Revoked {} tokens for user after password reset: userId={}", revokedCount, user.getUserId());
    }

    @Override
    public void invalidateResetToken(String resetToken) {
        if (resetToken == null || resetToken.trim().isEmpty() || redisTemplate == null) {
            return;
        }

        try {
            String jti = jwtTokenUtil.getJtiFromToken(resetToken);
            if (jti != null) {
                String tokenKey = RESET_TOKEN_PREFIX + jti;
                redisTemplate.delete(tokenKey);
                log.debug("Reset token invalidated: jti={}", jti);
            }
        } catch (Exception e) {
            log.warn("Error invalidating reset token: {}", e.getMessage());
        }
    }
}

