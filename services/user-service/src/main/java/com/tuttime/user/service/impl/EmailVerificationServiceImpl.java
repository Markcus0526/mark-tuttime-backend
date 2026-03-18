package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.user.entity.User;
import com.tuttime.user.exception.UserServiceMessageKeys;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.service.EmailVerificationService;
import com.tuttime.user.service.email.SmtpEmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Email Verification Service Implementation
 * 
 * <p>This service handles email code generation, storage, and verification.
 * 
 * <p><b>Features:</b>
 * <ul>
 *   <li>Generate 4-digit email codes</li>
 *   <li>Store codes in Redis with configurable TTL</li>
 *   <li>Verify email codes</li>
 *   <li>Track send attempts (max 3 per hour)</li>
 * </ul>
 * 
 * <p><b>Redis Key Structure:</b>
 * <ul>
 *   <li>{@code email:verification:{email}} → verification code (10-minute TTL)</li>
 *   <li>{@code email:attempts:{email}} → attempt count (1-hour TTL)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final UserRepository userRepository;
    private final SmtpEmailProvider smtpEmailProvider;

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @Value("${email.code.length:4}")
    private int codeLength;

    @Value("${email.code.expiration-minutes:10}")
    private int emailCodeExpirationMinutes;

    @Value("${email.max-attempts-per-hour:3}")
    private int maxAttemptsPerHour;

    /** Redis key prefix for email verification codes */
    private static final String EMAIL_VERIFICATION_PREFIX = "email:verification:";
    
    /** Redis key prefix for email send attempts */
    private static final String EMAIL_ATTEMPTS_PREFIX = "email:attempts:";

    private static final SecureRandom random = new SecureRandom();

    @Override
    public String generateCode() {
        int min = (int) Math.pow(10, codeLength - 1);
        int max = (int) Math.pow(10, codeLength) - 1;
        int code = min + random.nextInt(max - min + 1);
        // Ensure code is exactly codeLength digits by padding with leading zeros if needed
        return String.format("%0" + codeLength + "d", code);
    }

    @Override
    public void sendVerificationCode(String email, String code) throws BusinessException {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(
                    "Email is required",
                    UserServiceMessageKeys.EMAIL_REQUIRED);
        }

        // Check send attempts (rate limiting)
        if (!checkSendAttempts(email)) {
            throw new BusinessException(
                    String.format("Maximum email send attempts (%d per hour) exceeded. Please try again later.", 
                            maxAttemptsPerHour),
                    UserServiceMessageKeys.EMAIL_SEND_ATTEMPTS_EXCEEDED);
        }

        // Generate code if not provided
        String verificationCode = (code != null && !code.trim().isEmpty()) ? code : generateCode();

        // Store code in Redis with TTL
        if (redisTemplate != null) {
            String key = EMAIL_VERIFICATION_PREFIX + email.toLowerCase();
            redisTemplate.opsForValue().set(key, verificationCode, emailCodeExpirationMinutes, TimeUnit.MINUTES);
            log.info("Email code generated and stored for email: {}, expires in {} minutes", 
                    email, emailCodeExpirationMinutes);
        } else {
            log.warn("Redis not available - Email code not stored. Code: {} for email: {}", verificationCode, email);
        }

        // Send email via SendGrid
        try {
            smtpEmailProvider.sendVerificationCode(email, verificationCode);
            log.info("Verification code sent via email to {}", email);
        } catch (Exception e) {
            log.error("Error sending verification email to {}: {}", email, e.getMessage(), e);
            throw new BusinessException(
                    "Failed to send verification email. Please try again later.",
                    UserServiceMessageKeys.FAILED_TO_SEND_EMAIL);
        }

        // Increment send attempts counter
        incrementSendAttempts(email);
    }

    @Override
    public boolean verifyCode(String email, String code) throws BusinessException {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(
                    "Email is required",
                    UserServiceMessageKeys.EMAIL_REQUIRED);
        }
        if (code == null || code.trim().isEmpty()) {
            throw new BusinessException(
                    "Verification code is required",
                    UserServiceMessageKeys.EMAIL_CODE_REQUIRED);
        }

        if (redisTemplate == null) {
            throw new BusinessException(
                    "Email verification service is unavailable",
                    UserServiceMessageKeys.EMAIL_VERIFICATION_SERVICE_UNAVAILABLE);
        }

        // Get stored code from Redis (normalize email to lowercase)
        String key = EMAIL_VERIFICATION_PREFIX + email.toLowerCase();
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null) {
            log.warn("Email code not found or expired for email: {}", email);
            throw new BusinessException(
                    "Email code is invalid or has expired. Please request a new code.",
                    UserServiceMessageKeys.EMAIL_CODE_INVALID_OR_EXPIRED);
        }

        // Verify code matches
        if (!storedCode.equals(code)) {
            log.warn("Invalid email code provided for email: {}", email);
            throw new BusinessException(
                    "Invalid email code. Please try again.",
                    UserServiceMessageKeys.EMAIL_CODE_VERIFICATION_FAILED);
        }

        // Delete code from Redis after successful verification
        redisTemplate.delete(key);
        log.info("Email code verified successfully for email: {}", email);
        return true;
    }

    @Override
    @Transactional
    public void verifyEmailCode(String email, String code) throws BusinessException {
        // Verify the code first
        verifyCode(email, code);

        // Update user's emailVerified status to true if user exists
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (!user.getEmailVerified()) {
                user.setEmailVerified(true);
                userRepository.save(user);
                // Evict user cache to ensure fresh data on next /me request
                evictUserCache(user.getUserId());
                log.info("Email verified status updated to true for user: {} (email: {})", user.getUserId(), email);
            } else {
                log.debug("Email already verified for user: {} (email: {})", user.getUserId(), email);
            }
        } else {
            log.debug("User not found for email: {}, but code was verified. This is OK for signup flow.", email);
        }

        log.info("Email code verified successfully for email: {}", email);
    }

    @Override
    @Transactional
    public void verifyEmailCode(String email, String code, Long userId) throws BusinessException {
        if (userId == null) {
            throw new BusinessException(
                    "User ID is required",
                    UserServiceMessageKeys.USER_NOT_FOUND);
        }

        // Verify the code first
        verifyCode(email, code);

        // Verify that the email belongs to the authenticated user
        Optional<User> userOpt = userRepository.findByEmail(email.toLowerCase());
        if (userOpt.isEmpty()) {
            throw new BusinessException(
                    "Email not found",
                    UserServiceMessageKeys.EMAIL_REQUIRED);
        }

        User user = userOpt.get();
        if (!user.getUserId().equals(userId)) {
            throw new BusinessException(
                    "Email does not belong to the authenticated user",
                    UserServiceMessageKeys.EMAIL_ALREADY_REGISTERED);
        }

        // Update user's emailVerified status to true
        if (!user.getEmailVerified()) {
            user.setEmailVerified(true);
            userRepository.save(user);
            // Evict user cache to ensure fresh data on next /me request
            evictUserCache(userId);
            log.info("Email verified status updated to true for user: {} (email: {})", userId, email);
        } else {
            log.debug("Email already verified for user: {} (email: {})", userId, email);
        }

        log.info("Email code verified successfully for email: {} (userId: {})", email, userId);
    }

    @Override
    public String sendVerificationEmail(String email) throws BusinessException {
        return sendVerificationEmail(email, null);
    }

    @Override
    public String sendVerificationEmail(String email, Long userId) throws BusinessException {
        if (email == null || email.trim().isEmpty()) {
            throw new BusinessException(
                    "Email is required",
                    UserServiceMessageKeys.EMAIL_REQUIRED);
        }

        // Check if email already registered
        Optional<User> existingUser = userRepository.findByEmail(email.toLowerCase());
        if (existingUser.isPresent()) {
            // If userId is provided, check if the email belongs to that user
            if (userId != null && existingUser.get().getUserId().equals(userId)) {
                // User is verifying their own email - allow it
                log.info("User {} is verifying their own email: {}", userId, email);
            } else {
                // Email is registered to a different user
                throw new BusinessException(
                        "Email is already registered. Please sign in instead.",
                        UserServiceMessageKeys.EMAIL_ALREADY_REGISTERED);
            }
        }

        // Send verification code
        sendVerificationCode(email, null);
        
        return "Email verification code sent successfully";
    }

    /**
     * Checks if email send attempts are within rate limit.
     */
    private boolean checkSendAttempts(String email) {
        if (redisTemplate == null) {
            return true; // If Redis is unavailable, allow sending (but log warning)
        }

        String key = EMAIL_ATTEMPTS_PREFIX + email.toLowerCase();
        String attemptsStr = redisTemplate.opsForValue().get(key);
        
        if (attemptsStr == null) {
            return true; // No previous attempts
        }

        int attempts = Integer.parseInt(attemptsStr);
        return attempts < maxAttemptsPerHour;
    }

    /**
     * Increments email send attempts counter.
     */
    private void incrementSendAttempts(String email) {
        if (redisTemplate == null) {
            return;
        }

        String key = EMAIL_ATTEMPTS_PREFIX + email.toLowerCase();
        String attemptsStr = redisTemplate.opsForValue().get(key);
        
        int attempts = (attemptsStr == null) ? 1 : Integer.parseInt(attemptsStr) + 1;
        redisTemplate.opsForValue().set(key, String.valueOf(attempts), 1, TimeUnit.HOURS);
    }

    @Override
    public void sendProfileVerificationCode(Long userId) throws BusinessException {
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }

        // Find user by ID
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new BusinessException(
                        "User not found",
                        UserServiceMessageKeys.USER_NOT_FOUND));

        // Check if user has email
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new BusinessException("User does not have an email address");
        }

        // Send verification code to user's email
        sendVerificationCode(user.getEmail(), null);
        log.info("Profile email verification code sent to user: {} (email: {})", userId, user.getEmail());
    }

    /**
     * Evicts the user cache for the given user ID.
     * This ensures that after email verification, the /me endpoint returns fresh data.
     * 
     * @param userId The user ID to evict from cache
     */
    private void evictUserCache(Long userId) {
        if (cacheManager != null && userId != null) {
            try {
                // Evict user cache (used by /me endpoint)
                var usersCache = cacheManager.getCache("users");
                if (usersCache != null) {
                    usersCache.evict(userId);
                    log.debug("Evicted user cache for userId: {}", userId);
                } else {
                    log.debug("Cache 'users' not found in CacheManager");
                }
            } catch (Exception e) {
                log.warn("Failed to evict user cache for userId: {} - {}", userId, e.getMessage());
            }
        } else {
            if (cacheManager == null) {
                log.debug("CacheManager not available - cannot evict cache for userId: {}", userId);
            }
        }
    }
}

