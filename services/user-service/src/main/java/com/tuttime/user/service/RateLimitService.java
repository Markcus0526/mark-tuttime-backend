package com.tuttime.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiting Service
 * 
 * <p>Provides Redis-based rate limiting for API endpoints to prevent abuse
 * and ensure fair resource usage. Uses sliding window algorithm.
 * 
 * <p><b>Rate Limit Strategy:</b>
 * <ul>
 *   <li>Map queries: Configurable via {@code ratelimit.map.query.limit} (default: 100) requests per window</li>
 *   <li>Map query window: Configurable via {@code ratelimit.map.query.window-minutes} (default: 1 minute)</li>
 *   <li>Location updates: Configurable via {@code ratelimit.location.update.limit} (default: 10) requests per window</li>
 *   <li>Location update window: Configurable via {@code ratelimit.location.update.window-hours} (default: 1 hour)</li>
 * </ul>
 * 
 * <p><b>Redis Key Structure:</b>
 * <ul>
 *   <li>{@code ratelimit:map:{userId}:{ip}} → request_count</li>
 *   <li>{@code ratelimit:location:{userId}} → request_count</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;

    // Rate limit configuration (configurable via environment variables)
    @Value("${ratelimit.map.query.limit:100}")
    private int mapQueryLimit; // requests per minute

    @Value("${ratelimit.map.query.window-minutes:1}")
    private int mapQueryWindowMinutes;

    @Value("${ratelimit.location.update.limit:10}")
    private int locationUpdateLimit; // requests per hour

    @Value("${ratelimit.location.update.window-hours:1}")
    private int locationUpdateWindowHours;

    /**
     * Checks if map query is allowed for the given user and IP.
     * 
     * @param userId User ID (can be null for unauthenticated requests)
     * @param ip IP address
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isMapQueryAllowed(Long userId, String ip) {
        if (redisTemplate == null) {
            log.warn("Redis not available - rate limiting skipped for map query: userId={}, ip={}", userId, ip);
            return true; // Allow request if Redis unavailable
        }

        if (ip == null || ip.trim().isEmpty()) {
            log.warn("IP address is null or empty - allowing request");
            return true;
        }

        try {
            String key = buildMapQueryKey(userId, ip);
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            
            // Get current count
            String countStr = ops.get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            // Check if limit exceeded
            if (currentCount >= mapQueryLimit) {
                log.debug("Map query rate limit exceeded: userId={}, ip={}, count={}", userId, ip, currentCount);
                return false;
            }
            
            // Increment counter
            ops.increment(key);
            Duration mapQueryWindow = Duration.ofMinutes(mapQueryWindowMinutes);
            redisTemplate.expire(key, mapQueryWindow.toSeconds(), TimeUnit.SECONDS);
            
            return true;
        } catch (Exception e) {
            log.error("Error checking map query rate limit: userId={}, ip={}", userId, ip, e);
            // On error, allow request to prevent service disruption
            return true;
        }
    }

    /**
     * Checks if location update is allowed for the given user.
     * 
     * @param userId User ID
     * @return true if request is allowed, false if rate limit exceeded
     */
    public boolean isLocationUpdateAllowed(Long userId) {
        if (redisTemplate == null) {
            log.warn("Redis not available - rate limiting skipped for location update: userId={}", userId);
            return true; // Allow request if Redis unavailable
        }

        if (userId == null) {
            log.warn("User ID is null - allowing request");
            return true;
        }

        try {
            String key = buildLocationUpdateKey(userId);
            ValueOperations<String, String> ops = redisTemplate.opsForValue();
            
            // Get current count
            String countStr = ops.get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            
            // Check if limit exceeded
            if (currentCount >= locationUpdateLimit) {
                log.debug("Location update rate limit exceeded: userId={}, count={}", userId, currentCount);
                return false;
            }
            
            // Increment counter
            ops.increment(key);
            Duration locationUpdateWindow = Duration.ofHours(locationUpdateWindowHours);
            redisTemplate.expire(key, locationUpdateWindow.toSeconds(), TimeUnit.SECONDS);
            
            return true;
        } catch (Exception e) {
            log.error("Error checking location update rate limit: userId={}", userId, e);
            // On error, allow request to prevent service disruption
            return true;
        }
    }

    /**
     * Gets remaining requests for map queries.
     * 
     * @param userId User ID (can be null)
     * @param ip IP address
     * @return Remaining requests in current window
     */
    public int getRemainingMapQueries(Long userId, String ip) {
        if (redisTemplate == null || ip == null || ip.trim().isEmpty()) {
            return mapQueryLimit;
        }

        try {
            String key = buildMapQueryKey(userId, ip);
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            return Math.max(0, mapQueryLimit - currentCount);
        } catch (Exception e) {
            log.error("Error getting remaining map queries: userId={}, ip={}", userId, ip, e);
            return mapQueryLimit;
        }
    }

    /**
     * Gets the configured map query limit.
     * 
     * @return Map query limit per window
     */
    public int getMapQueryLimit() {
        return mapQueryLimit;
    }

    /**
     * Gets the configured location update limit.
     * 
     * @return Location update limit per window
     */
    public int getLocationUpdateLimit() {
        return locationUpdateLimit;
    }

    /**
     * Gets remaining requests for location updates.
     * 
     * @param userId User ID
     * @return Remaining requests in current window
     */
    public int getRemainingLocationUpdates(Long userId) {
        if (redisTemplate == null || userId == null) {
            return locationUpdateLimit;
        }

        try {
            String key = buildLocationUpdateKey(userId);
            String countStr = redisTemplate.opsForValue().get(key);
            int currentCount = countStr != null ? Integer.parseInt(countStr) : 0;
            return Math.max(0, locationUpdateLimit - currentCount);
        } catch (Exception e) {
            log.error("Error getting remaining location updates: userId={}", userId, e);
            return locationUpdateLimit;
        }
    }

    /**
     * Builds Redis key for map query rate limiting.
     */
    private String buildMapQueryKey(Long userId, String ip) {
        if (userId != null) {
            return "ratelimit:map:user:" + userId + ":ip:" + ip;
        } else {
            return "ratelimit:map:ip:" + ip;
        }
    }

    /**
     * Builds Redis key for location update rate limiting.
     */
    private String buildLocationUpdateKey(Long userId) {
        return "ratelimit:location:" + userId;
    }
}

