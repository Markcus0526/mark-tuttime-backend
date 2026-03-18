package com.tuttime.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Cache Clear Service
 * 
 * <p>This service provides functionality to clear all Redis caches including:
 * <ul>
 *   <li>SMS verification codes and rate limits</li>
 *   <li>Email verification codes and rate limits</li>
 *   <li>Signup tokens</li>
 *   <li>Rate limiting counters</li>
 *   <li>Brute force protection data</li>
 *   <li>Password reset tokens</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheClearService {

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    // Redis key prefixes
    private static final String SMS_VERIFICATION_PREFIX = "sms:verification:";
    private static final String SMS_SIGNUP_PREFIX = "sms:signup:";
    private static final String SMS_ATTEMPTS_PREFIX = "sms:attempts:";
    private static final String SIGNUP_TOKEN_PREFIX = "signup:token:";
    private static final String EMAIL_VERIFICATION_PREFIX = "email:verification:";
    private static final String EMAIL_ATTEMPTS_PREFIX = "email:attempts:";
    private static final String RATE_LIMIT_PREFIX = "ratelimit:";
    private static final String BRUTEFORCE_PREFIX = "bruteforce:";
    private static final String PASSWORD_RESET_PREFIX = "password-reset:token:";
    private static final String PASSWORD_RESET_RETRY_PREFIX = "password-reset:retry:";
    private static final String PASSWORD_RESET_CHANNEL_PREFIX = "password-reset:channel:";

    /**
     * Clears all caches including verification codes, rate limits, and tokens.
     * 
     * @return Number of keys deleted
     */
    public int clearAllCaches() {
        if (redisTemplate == null) {
            log.warn("Redis not available - cannot clear caches");
            return 0;
        }

        int totalDeleted = 0;

        try {
            // Clear SMS verification codes
            totalDeleted += deleteKeysByPattern(SMS_VERIFICATION_PREFIX + "*");
            log.info("Cleared SMS verification codes");

            // Clear SMS signup codes
            totalDeleted += deleteKeysByPattern(SMS_SIGNUP_PREFIX + "*");
            log.info("Cleared SMS signup codes");

            // Clear SMS send attempts (rate limiting)
            totalDeleted += deleteKeysByPattern(SMS_ATTEMPTS_PREFIX + "*");
            log.info("Cleared SMS send attempts (rate limits)");

            // Clear signup tokens
            totalDeleted += deleteKeysByPattern(SIGNUP_TOKEN_PREFIX + "*");
            log.info("Cleared signup tokens");

            // Clear email verification codes
            totalDeleted += deleteKeysByPattern(EMAIL_VERIFICATION_PREFIX + "*");
            log.info("Cleared email verification codes");

            // Clear email send attempts (rate limiting)
            totalDeleted += deleteKeysByPattern(EMAIL_ATTEMPTS_PREFIX + "*");
            log.info("Cleared email send attempts (rate limits)");

            // Clear rate limiting counters
            totalDeleted += deleteKeysByPattern(RATE_LIMIT_PREFIX + "*");
            log.info("Cleared rate limiting counters");

            // Clear brute force protection data
            totalDeleted += deleteKeysByPattern(BRUTEFORCE_PREFIX + "*");
            log.info("Cleared brute force protection data");

            // Clear password reset tokens
            totalDeleted += deleteKeysByPattern(PASSWORD_RESET_PREFIX + "*");
            log.info("Cleared password reset tokens");

            // Clear password reset retry state and channel
            totalDeleted += deleteKeysByPattern(PASSWORD_RESET_RETRY_PREFIX + "*");
            totalDeleted += deleteKeysByPattern(PASSWORD_RESET_CHANNEL_PREFIX + "*");
            log.info("Cleared password reset retry state and channel");

            log.info("Cache clear completed. Total keys deleted: {}", totalDeleted);
            return totalDeleted;

        } catch (Exception e) {
            log.error("Error clearing caches", e);
            throw new RuntimeException("Failed to clear caches: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes all keys matching the given pattern.
     * 
     * @param pattern Redis key pattern (e.g., "sms:verification:*")
     * @return Number of keys deleted
     */
    private int deleteKeysByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                return keys.size();
            }
            return 0;
        } catch (Exception e) {
            log.error("Error deleting keys with pattern: {}", pattern, e);
            return 0;
        }
    }
}

