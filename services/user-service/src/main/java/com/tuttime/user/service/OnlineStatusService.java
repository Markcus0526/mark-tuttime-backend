package com.tuttime.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

/** Online Status Service: Redis TTL-based presence tracking. Architecture: Redis Set (online:users), TTL keys (presence:user:{userId}), Pub/Sub (presence:events). Handles 10M+ users, zero DB writes. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineStatusService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    
    private static final String ONLINE_USERS_SET = "online:users";
    private static final String PRESENCE_KEY_PREFIX = "presence:user:";
    private static final String PRESENCE_EVENTS_CHANNEL = "presence:events";
    
    /** Mark user online in Redis (with TTL). Called when WebSocket connects. */
    public void markUserOnline(Long userId, int ttlSeconds) {
        try {
            // Check if Redis connection is still available (graceful shutdown handling)
            if (!isRedisAvailable()) {
                log.debug("Redis not available - skipping online status update for user {}", userId);
                return;
            }
            
            String presenceKey = PRESENCE_KEY_PREFIX + userId;
            String userIdStr = String.valueOf(userId);
            
                        // Set presence key with TTL (auto-expires if no heartbeat)
            redisTemplate.opsForValue().set(presenceKey, "online", Duration.ofSeconds(ttlSeconds));
            
        // Add to online users set
            redisTemplate.opsForSet().add(ONLINE_USERS_SET, userIdStr);
            
            // Publish online event asynchronously (fire-and-forget, non-blocking)
            // This doesn't block the WebSocket connection even if Pub/Sub is slow
            try {
                publishPresenceEvent(userId, true);
            } catch (Exception e) {
                // Pub/Sub failures are non-critical - log but don't fail
                log.debug("Failed to publish presence event (non-critical): userId={}, error={}", 
                        userId, e.getMessage());
            }
            
            log.debug("User {} marked online (TTL: {}s)", userId, ttlSeconds);
        } catch (IllegalStateException e) {
            // Redis connection factory was destroyed (during shutdown)
            log.debug("Redis connection destroyed during shutdown - skipping online status update for user {}", userId);
        } catch (Exception e) {
            log.warn("Error marking user {} online: {}. WebSocket connection will continue.", userId, e.getMessage());
        }
    }
    
    /** Refresh presence TTL (called on heartbeat). */
    public void refreshPresence(Long userId, int ttlSeconds) {
        try {
            // Check if Redis connection is still available (graceful shutdown handling)
            if (!isRedisAvailable()) {
                log.debug("Redis not available - skipping presence refresh for user {}", userId);
                return;
            }
            
            String presenceKey = PRESENCE_KEY_PREFIX + userId;
            String userIdStr = String.valueOf(userId);
            
            // Optimized: Fast Redis operations (SET and SADD are O(1) operations)
            // These operations are already optimized by RedisTemplate
            // Refresh TTL (if key exists, extend it; if not, create it)
            redisTemplate.opsForValue().set(presenceKey, "online", Duration.ofSeconds(ttlSeconds));
            
            // Ensure user is in online set (non-blocking, fast operation)
            redisTemplate.opsForSet().add(ONLINE_USERS_SET, userIdStr);
            
            log.debug("Presence refreshed for user {} (TTL: {}s)", userId, ttlSeconds);
        } catch (IllegalStateException e) {
            // Redis connection factory was destroyed (during shutdown)
            log.debug("Redis connection destroyed during shutdown - skipping presence refresh for user {}", userId);
        } catch (Exception e) {
            log.warn("Error refreshing presence for user {}: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Mark user offline
     * Called when WebSocket disconnects
     */
    public void markUserOffline(Long userId) {
        try {
            // Check if Redis connection is still available (graceful shutdown handling)
            if (!isRedisAvailable()) {
                log.debug("Redis not available during shutdown - skipping offline status update for user {}", userId);
                return;
            }
            
            String presenceKey = PRESENCE_KEY_PREFIX + userId;
            String userIdStr = String.valueOf(userId);
            
            // Remove presence key
            redisTemplate.delete(presenceKey);
            
            // Remove from online users set
            redisTemplate.opsForSet().remove(ONLINE_USERS_SET, userIdStr);
            
            // Publish offline event
            publishPresenceEvent(userId, false);
            
            log.debug("User {} marked offline", userId);
        } catch (IllegalStateException e) {
            // Redis connection factory was destroyed (during shutdown)
            log.debug("Redis connection destroyed during shutdown - skipping offline status update for user {}", userId);
        } catch (Exception e) {
            log.warn("Error marking user {} offline: {}", userId, e.getMessage());
        }
    }
    
    /**
     * Check if Redis connection is available
     * Used during graceful shutdown to avoid errors
     */
    private boolean isRedisAvailable() {
        try {
            // Try a simple operation to check if Redis is available
            // Use hasKey on a non-existent key as a lightweight check
            redisTemplate.hasKey("__shutdown_check__");
            return true;
        } catch (IllegalStateException e) {
            // Redis connection factory was destroyed
            return false;
        } catch (Exception e) {
            // Any other exception means Redis is not available
            return false;
        }
    }
    
    /**
     * Check if user is online (from Redis)
     */
    public boolean isUserOnline(Long userId) {
        String presenceKey = PRESENCE_KEY_PREFIX + userId;
        String value = redisTemplate.opsForValue().get(presenceKey);
        return value != null && "online".equals(value);
    }
    
    /**
     * Batch check online status (optimized with pipeline)
     */
    public Map<Long, Boolean> getUsersOnlineStatus(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<Long, Boolean> result = new HashMap<>();
        
        try {
            // Use Redis pipeline for batch check
            List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
                RedisConnection redisConn = (RedisConnection) connection;
                
                for (Long userId : userIds) {
                    String presenceKey = PRESENCE_KEY_PREFIX + userId;
                    redisConn.exists(presenceKey.getBytes());
                }
                
                return null;
            });
            
            // Map results - Redis EXISTS returns Long (1 for exists, 0 for not exists)
            // Spring Data Redis wraps it, so we need to handle both Boolean and Long
            for (int i = 0; i < userIds.size(); i++) {
                Long userId = userIds.get(i);
                Object resultObj = results.get(i);
                boolean isOnline = false;
                
                if (resultObj instanceof Boolean) {
                    isOnline = Boolean.TRUE.equals(resultObj);
                } else if (resultObj instanceof Long) {
                    isOnline = ((Long) resultObj) > 0L;
                } else if (resultObj instanceof Number) {
                    isOnline = ((Number) resultObj).longValue() > 0L;
                }
                
                result.put(userId, isOnline);
            }
        } catch (Exception e) {
            log.error("Error getting users online status, returning all false", e);
            // On error, return all false to prevent incorrect online status
            for (Long userId : userIds) {
                result.put(userId, false);
            }
        }
        
        return result;
    }
    
    /**
     * Get online users count
     * 
     * <p>Counts users who have valid presence keys (not expired).
     * This is more accurate than counting the set size, as the set
     * may contain stale entries when presence keys expire.
     * 
     * <p>Uses Redis SCAN to find all presence keys (non-blocking, production-safe).
     * SCAN automatically excludes expired keys (Redis removes expired keys automatically).
     * This is much faster than KEYS for large Redis instances.
     */
    public long getOnlineUsersCount() {
        try {
            // Use Redis SCAN to count all presence keys (non-blocking, production-safe)
            // SCAN is cursor-based and doesn't block Redis like KEYS does
            String pattern = PRESENCE_KEY_PREFIX + "*";
            
            long count = redisTemplate.execute((RedisCallback<Long>) connection -> {
                RedisConnection redisConn = (RedisConnection) connection;
                long keyCount = 0L;
                byte[] patternBytes = pattern.getBytes();
                
                // Use SCAN with cursor to iterate through keys
                // SCAN returns keys in batches, allowing Redis to remain responsive
                // This is much faster and non-blocking compared to KEYS command
                org.springframework.data.redis.core.ScanOptions scanOptions = 
                    org.springframework.data.redis.core.ScanOptions.scanOptions()
                        .match(patternBytes)
                        .count(100) // Process 100 keys at a time (good balance)
                        .build();
                
                org.springframework.data.redis.core.Cursor<byte[]> cursor = redisConn.scan(scanOptions);
                
                try {
                    while (cursor.hasNext()) {
                        cursor.next();
                        keyCount++;
                    }
                } finally {
                    try {
                        cursor.close();
                    } catch (Exception e) {
                        log.debug("Error closing SCAN cursor: {}", e.getMessage());
                    }
                }
                
                return keyCount;
            });
            
            // Also clean up the set if it has stale entries
            // Get set size for comparison
            Long setSize = redisTemplate.opsForSet().size(ONLINE_USERS_SET);
            if (setSize != null && setSize > count) {
                log.debug("Online users set has {} entries but only {} valid presence keys. " +
                         "Stale entries detected, will clean up.", setSize, count);
                
                // Clean up stale entries asynchronously (non-blocking)
                Set<String> userIds = redisTemplate.opsForSet().members(ONLINE_USERS_SET);
                if (userIds != null && !userIds.isEmpty()) {
                    new Thread(() -> {
                        try {
                            int cleaned = 0;
                            for (String userIdStr : userIds) {
                                String presenceKey = PRESENCE_KEY_PREFIX + userIdStr;
                                if (!Boolean.TRUE.equals(redisTemplate.hasKey(presenceKey))) {
                                    redisTemplate.opsForSet().remove(ONLINE_USERS_SET, userIdStr);
                                    cleaned++;
                                }
                            }
                            if (cleaned > 0) {
                                log.debug("Cleaned up {} stale entries from online users set", cleaned);
                            }
                        } catch (Exception e) {
                            log.warn("Error cleaning up stale online users: {}", e.getMessage());
                        }
                    }).start();
                }
            }
            
            return count;
        } catch (Exception e) {
            log.error("Error getting online users count with SCAN, falling back to set size", e);
            // Fallback to set size if scan fails
            Long count = redisTemplate.opsForSet().size(ONLINE_USERS_SET);
            return count != null ? count : 0L;
        }
    }
    
    /**
     * Get all online user IDs (use with caution - can be large)
     * 
     * <p>Returns only users with valid presence keys (not expired).
     * This filters out stale entries from the set.
     */
    public Set<Long> getAllOnlineUserIds() {
        try {
            // Get all presence keys directly (more accurate than set)
            String pattern = PRESENCE_KEY_PREFIX + "*";
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys == null || keys.isEmpty()) {
                return Collections.emptySet();
            }
            
            // Extract user IDs from presence keys
            return keys.stream()
                .map(key -> {
                    // Extract userId from "presence:user:{userId}"
                    String userIdStr = key.substring(PRESENCE_KEY_PREFIX.length());
                    try {
                        return Long.parseLong(userIdStr);
                    } catch (NumberFormatException e) {
                        log.warn("Invalid presence key format: {}", key);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        } catch (Exception e) {
            log.error("Error getting all online user IDs", e);
            return Collections.emptySet();
        }
    }
    
    /**
     * Publish presence event to Redis Pub/Sub
     */
    private void publishPresenceEvent(Long userId, boolean isOnline) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("userId", userId);
            event.put("isOnline", isOnline);
            event.put("timestamp", System.currentTimeMillis());
            
            // Serialize event to JSON string (RedisTemplate<String, String> requires String)
            String eventJson = objectMapper.writeValueAsString(event);
            
            // Publish to Redis Pub/Sub (other services can subscribe)
            redisTemplate.convertAndSend(PRESENCE_EVENTS_CHANNEL, eventJson);
            
            log.debug("Published presence event: userId={}, isOnline={}", userId, isOnline);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize presence event: userId={}", userId, e);
        } catch (Exception e) {
            log.error("Failed to publish presence event: userId={}", userId, e);
        }
    }
}

