package com.tuttime.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/** Brute-Force Protection Service: tracks failed login attempts, blocks IP/phone after thresholds. Rules: 3=warning, 5=IP block 15min, 10=phone block 1h, 15=permanent block. */
@Slf4j
@Service
@RequiredArgsConstructor
public class BruteForceProtectionService {

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    @Value("${bruteforce.warning-threshold:3}")
    private int warningThreshold;

    @Value("${bruteforce.ip-block-threshold:5}")
    private int ipBlockThreshold;

    @Value("${bruteforce.phone-block-threshold:10}")
    private int phoneBlockThreshold;

    @Value("${bruteforce.permanent-block-threshold:15}")
    private int permanentBlockThreshold;

    @Value("${bruteforce.ip-block-duration-minutes:15}")
    private long ipBlockDurationMinutes;

    @Value("${bruteforce.phone-block-duration-minutes:60}")
    private long phoneBlockDurationMinutes;

    private static final String ATTEMPT_COUNTER_PREFIX = "bruteforce:login:";
    private static final String BLOCKED_IP_PREFIX = "bruteforce:blocked:ip:";
    private static final String BLOCKED_PHONE_PREFIX = "bruteforce:blocked:phone:";

    /** Records failed login attempt: increments counter, applies blocking rules if thresholds exceeded. */
    public void recordFailedAttempt(String ip, String phoneNumber) {
        if (redisTemplate == null) {
            log.warn("Redis not available - brute-force protection skipped for IP: {}, phone: {}", ip, phoneNumber);
            return;
        }

        if (ip == null || ip.trim().isEmpty()) {
            log.warn("Cannot record failed attempt: IP is null or empty");
            return;
        }

        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            log.warn("Cannot record failed attempt: Phone number is null or empty");
            return;
        }

        try {
            String attemptKey = ATTEMPT_COUNTER_PREFIX + ip + ":" + phoneNumber;
            
            // Increment attempt counter (expires after 24 hours)
            Long attempts = redisTemplate.opsForValue().increment(attemptKey);
            redisTemplate.expire(attemptKey, 24, TimeUnit.HOURS);
            
            if (attempts == null) {
                attempts = 1L;
            }

            log.debug("Failed login attempt recorded: IP={}, phone={}, attempts={}", ip, phoneNumber, attempts);

            // Apply blocking rules based on attempt count
            if (attempts >= permanentBlockThreshold) {
                // Permanent block - requires admin review
                blockPermanently(ip, phoneNumber);
                log.warn("PERMANENT BLOCK: IP={}, phone={}, attempts={} - Admin review required", 
                        ip, phoneNumber, attempts);
            } else if (attempts >= phoneBlockThreshold) {
                // Block phone number for 1 hour
                blockPhoneNumber(phoneNumber, phoneBlockDurationMinutes);
                log.warn("Phone number blocked: phone={}, attempts={}, blockDuration={} minutes", 
                        phoneNumber, attempts, phoneBlockDurationMinutes);
            } else if (attempts >= ipBlockThreshold) {
                // Block IP address for 15 minutes
                blockIpAddress(ip, ipBlockDurationMinutes);
                log.warn("IP address blocked: IP={}, attempts={}, blockDuration={} minutes", 
                        ip, attempts, ipBlockDurationMinutes);
            } else if (attempts >= warningThreshold) {
                // Log warning only
                log.warn("Brute-force warning: IP={}, phone={}, attempts={}", ip, phoneNumber, attempts);
            }

        } catch (Exception e) {
            log.error("Error recording failed attempt: IP={}, phone={}", ip, phoneNumber, e);
        }
    }

    /**
     * Checks if the given IP address or phone number is currently blocked.
     *
     * @param ip IP address to check
     * @param phoneNumber Phone number to check
     * @return true if either IP or phone number is blocked, false otherwise
     */
    public boolean isBlocked(String ip, String phoneNumber) {
        if (redisTemplate == null) {
            log.warn("Redis not available - cannot check brute-force protection. Assuming not blocked.");
            return false;
        }

        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        // Check if IP is blocked
        if (isIpBlocked(ip)) {
            return true;
        }

        // Check if phone number is blocked
        if (phoneNumber != null && !phoneNumber.trim().isEmpty() && isPhoneBlocked(phoneNumber)) {
            return true;
        }

        return false;
    }

    /**
     * Gets the remaining block time in seconds for the given IP or phone number.
     * 
     * <p>Returns the time until the block expires, or 0 if not blocked.
     *
     * @param ip IP address to check
     * @param phoneNumber Phone number to check
     * @return Remaining block time in seconds, or 0 if not blocked
     */
    public long getRemainingBlockTime(String ip, String phoneNumber) {
        if (redisTemplate == null) {
            return 0;
        }

        long remainingTime = 0;

        // Check IP block time
        if (ip != null && !ip.trim().isEmpty()) {
            String ipBlockKey = BLOCKED_IP_PREFIX + ip;
            String ipBlockUntil = redisTemplate.opsForValue().get(ipBlockKey);
            if (ipBlockUntil != null) {
                try {
                    long blockUntil = Long.parseLong(ipBlockUntil);
                    long now = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
                    remainingTime = Math.max(remainingTime, blockUntil - now);
                } catch (NumberFormatException e) {
                    log.warn("Invalid block timestamp for IP: {}", ip, e);
                }
            }
        }

        // Check phone block time
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            String phoneBlockKey = BLOCKED_PHONE_PREFIX + phoneNumber;
            String phoneBlockUntil = redisTemplate.opsForValue().get(phoneBlockKey);
            if (phoneBlockUntil != null) {
                try {
                    long blockUntil = Long.parseLong(phoneBlockUntil);
                    long now = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
                    remainingTime = Math.max(remainingTime, blockUntil - now);
                } catch (NumberFormatException e) {
                    log.warn("Invalid block timestamp for phone: {}", phoneNumber, e);
                }
            }
        }

        return Math.max(0, remainingTime);
    }

    /**
     * Resets failed attempt counters for the given IP and phone number.
     * 
     * <p>Called on successful login to clear any accumulated failed attempts.
     *
     * @param ip IP address to reset
     * @param phoneNumber Phone number to reset
     */
    public void resetAttempts(String ip, String phoneNumber) {
        if (redisTemplate == null) {
            log.warn("Redis not available - cannot reset brute-force attempts for IP: {}, phone: {}", ip, phoneNumber);
            return;
        }

        if (ip == null || ip.trim().isEmpty()) {
            return;
        }

        try {
            // Reset attempt counter
            if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
                String attemptKey = ATTEMPT_COUNTER_PREFIX + ip + ":" + phoneNumber;
                redisTemplate.delete(attemptKey);
                log.debug("Reset failed attempts for IP: {}, phone: {}", ip, phoneNumber);
            }
        } catch (Exception e) {
            log.error("Error resetting failed attempts: IP={}, phone={}", ip, phoneNumber, e);
        }
    }

    /**
     * Checks if an IP address is currently blocked.
     *
     * @param ip IP address to check
     * @return true if IP is blocked, false otherwise
     */
    private boolean isIpBlocked(String ip) {
        String ipBlockKey = BLOCKED_IP_PREFIX + ip;
        String blockUntil = redisTemplate.opsForValue().get(ipBlockKey);
        
        if (blockUntil == null) {
            return false;
        }

        try {
            long blockUntilTimestamp = Long.parseLong(blockUntil);
            long now = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
            
            if (blockUntilTimestamp > now) {
                return true; // Still blocked
            } else {
                // Block expired, remove it
                redisTemplate.delete(ipBlockKey);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid block timestamp for IP: {}", ip, e);
            // Remove invalid entry
            redisTemplate.delete(ipBlockKey);
            return false;
        }
    }

    /**
     * Checks if a phone number is currently blocked.
     *
     * @param phoneNumber Phone number to check
     * @return true if phone number is blocked, false otherwise
     */
    private boolean isPhoneBlocked(String phoneNumber) {
        String phoneBlockKey = BLOCKED_PHONE_PREFIX + phoneNumber;
        String blockUntil = redisTemplate.opsForValue().get(phoneBlockKey);
        
        if (blockUntil == null) {
            return false;
        }

        try {
            long blockUntilTimestamp = Long.parseLong(blockUntil);
            long now = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
            
            if (blockUntilTimestamp > now) {
                return true; // Still blocked
            } else {
                // Block expired, remove it
                redisTemplate.delete(phoneBlockKey);
                return false;
            }
        } catch (NumberFormatException e) {
            log.warn("Invalid block timestamp for phone: {}", phoneNumber, e);
            // Remove invalid entry
            redisTemplate.delete(phoneBlockKey);
            return false;
        }
    }

    /**
     * Blocks an IP address for the specified duration.
     *
     * @param ip IP address to block
     * @param durationMinutes Block duration in minutes
     */
    private void blockIpAddress(String ip, long durationMinutes) {
        String ipBlockKey = BLOCKED_IP_PREFIX + ip;
        long blockUntil = (System.currentTimeMillis() / 1000) + (durationMinutes * 60); // Unix timestamp in seconds
        redisTemplate.opsForValue().set(ipBlockKey, String.valueOf(blockUntil), 
                durationMinutes + 1, TimeUnit.MINUTES); // Add 1 minute buffer for expiration
    }

    /**
     * Blocks a phone number for the specified duration.
     *
     * @param phoneNumber Phone number to block
     * @param durationMinutes Block duration in minutes
     */
    private void blockPhoneNumber(String phoneNumber, long durationMinutes) {
        String phoneBlockKey = BLOCKED_PHONE_PREFIX + phoneNumber;
        long blockUntil = (System.currentTimeMillis() / 1000) + (durationMinutes * 60); // Unix timestamp in seconds
        redisTemplate.opsForValue().set(phoneBlockKey, String.valueOf(blockUntil), 
                durationMinutes + 1, TimeUnit.MINUTES); // Add 1 minute buffer for expiration
    }

    /**
     * Permanently blocks an IP address and phone number.
     * 
     * <p>Sets a very long block duration (10 years) to effectively make it permanent.
     * Requires admin review to unblock.
     *
     * @param ip IP address to block
     * @param phoneNumber Phone number to block
     */
    private void blockPermanently(String ip, String phoneNumber) {
        // Block IP permanently (10 years)
        blockIpAddress(ip, 10 * 365 * 24 * 60); // 10 years in minutes
        
        // Block phone number permanently (10 years)
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            blockPhoneNumber(phoneNumber, 10 * 365 * 24 * 60); // 10 years in minutes
        }
    }
}

