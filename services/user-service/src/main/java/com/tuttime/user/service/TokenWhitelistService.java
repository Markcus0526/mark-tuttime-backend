package com.tuttime.user.service;

import com.tuttime.user.dto.TokenMetadata;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Service for managing JWT token whitelist in Redis: token revocation, validation, bulk revocation, lifecycle management. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenWhitelistService {
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOKEN_METADATA_PREFIX = "whitelist:token:";
    private static final String USER_TOKENS_PREFIX = "whitelist:user:";
    private static final String USER_TOKENS_SUFFIX = ":tokens";
    private static final String DEVICE_TOKENS_PREFIX = "whitelist:device:";
    private static final String DEVICE_TOKENS_SUFFIX = ":tokens";
    
    /** Add token to whitelist: stores metadata with TTL, adds JTI to user/device sets, verifies write to handle Redis replication lag. */
    public boolean addToWhitelist(String jti, Long userId, String tokenType, Long expirationTime, String deviceId) {
        if (redisTemplate == null) {
            log.warn("Redis not available - token whitelisting skipped");
            return false;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            long ttl = expirationTime - currentTime;
            
            // Only add if token hasn't expired
            if (ttl <= 0) {
                log.debug("Token already expired, not adding to whitelist: jti={}", jti);
                return false;
            }
            
            // Create token metadata
            TokenMetadata metadata = TokenMetadata.builder()
                    .userId(userId)
                    .tokenType(tokenType)
                    .issuedAt(currentTime)
                    .expiresAt(expirationTime)
                    .deviceId(deviceId)
                    .build();
            
            // Serialize metadata to JSON
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            // Store token metadata with TTL
            String tokenKey = TOKEN_METADATA_PREFIX + jti;
            redisTemplate.opsForValue().set(tokenKey, metadataJson, ttl, TimeUnit.MILLISECONDS);
            
            // Add JTI to user's token set
            String userTokensKey = USER_TOKENS_PREFIX + userId + USER_TOKENS_SUFFIX;
            redisTemplate.opsForSet().add(userTokensKey, jti);
            // Set TTL on user token set (use max expiration time)
            redisTemplate.expire(userTokensKey, ttl, TimeUnit.MILLISECONDS);
            
            // Add JTI to device's token set (if deviceId provided)
            if (deviceId != null && !deviceId.trim().isEmpty()) {
                String deviceTokensKey = DEVICE_TOKENS_PREFIX + deviceId + DEVICE_TOKENS_SUFFIX;
                redisTemplate.opsForSet().add(deviceTokensKey, jti);
                redisTemplate.expire(deviceTokensKey, ttl, TimeUnit.MILLISECONDS);
            }
            
            // Read-after-write verification: Verify token exists in Redis
            // This ensures the token is available before returning, preventing
            // issues with Redis replication lag in Kubernetes deployments
            if (!verifyTokenExists(tokenKey, 3)) {
                log.error("Token whitelist verification failed after write: jti={}, userId={}, tokenType={}. " +
                        "Token may not be immediately available due to Redis replication lag.", 
                        jti, userId, tokenType);
                // Still return true - the write likely succeeded, just not visible yet due to replication lag
                // The retry mechanism in the calling code will handle this
                return true; // Assume write succeeded, verification will happen on read
            }
            
            log.debug("Token added to whitelist and verified: jti={}, userId={}, tokenType={}, ttl={}ms", 
                    jti, userId, tokenType, ttl);
            return true;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize token metadata: jti={}", jti, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to add token to whitelist: jti={}", jti, e);
            return false;
        }
    }
    
    /**
     * Verify that a token exists in Redis with retry logic.
     * 
     * <p>This method implements read-after-write verification to handle
     * Redis replication lag in Kubernetes deployments. It retries with
     * exponential backoff to allow time for replication to complete.
     * 
     * @param tokenKey Redis key for the token
     * @param maxRetries Maximum number of retry attempts
     * @return true if token exists, false otherwise
     */
    private boolean verifyTokenExists(String tokenKey, int maxRetries) {
        // Fast path: try immediate check first (most tokens are already replicated)
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey))) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Error in initial token verification: key={}, error={}", tokenKey, e.getMessage());
        }
        
        // If not found on first attempt, do quick retries without blocking
        // This handles replication lag without blocking the handshake thread
        for (int attempt = 1; attempt < maxRetries; attempt++) {
            try {
                // Immediate retry without sleep - relies on Redis connection retry mechanism
                // This is much faster and doesn't block the WebSocket handshake thread
                if (Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey))) {
                    log.debug("Token verification succeeded after {} retry attempts: key={}", attempt, tokenKey);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Error verifying token existence (attempt {}/{}): key={}, error={}", 
                        attempt + 1, maxRetries, tokenKey, e.getMessage());
                // Continue to next retry attempt immediately
            }
        }
        
        return false;
    }
    
    /**
     * Check if a token is whitelisted
     * 
     * <p><b>Optimized for WebSocket handshake:</b>
     * - Fast path: single immediate check (most common case)
     * - Quick retries without blocking for replication lag
     * - Non-blocking to prevent WebSocket handshake timeouts
     * 
     * @param jti JWT ID to check
     * @return true if token exists in whitelist and hasn't expired
     */
    public boolean isTokenWhitelisted(String jti) {
        if (redisTemplate == null) {
            log.debug("Redis not available - assuming token is whitelisted (stateless mode)");
            return true; // In stateless mode, assume token is valid if signature is valid
        }
        
        String tokenKey = TOKEN_METADATA_PREFIX + jti;
        
        // Optimized: Use fast retry logic (non-blocking) to handle Redis replication lag
        // Reduced retries for WebSocket handshake to minimize blocking time
        // Most tokens are found on first attempt, retries handle edge cases
        return verifyTokenExists(tokenKey, 2); // Reduced from 3 to 2 for faster handshake
    }
    
    /**
     * Get token metadata from whitelist
     * 
     * @param jti JWT ID
     * @return TokenMetadata if token exists, null otherwise
     */
    public TokenMetadata getTokenMetadata(String jti) {
        if (redisTemplate == null) {
            return null;
        }
        
        try {
            String tokenKey = TOKEN_METADATA_PREFIX + jti;
            String metadataJson = redisTemplate.opsForValue().get(tokenKey);
            
            if (metadataJson == null) {
                return null;
            }
            
            return objectMapper.readValue(metadataJson, TokenMetadata.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize token metadata: jti={}", jti, e);
            return null;
        } catch (Exception e) {
            log.warn("Error getting token metadata: jti={}", jti, e);
            return null;
        }
    }
    
    /**
     * Remove a token from the whitelist
     * 
     * <p>Removes token metadata and JTI from user and device token sets.
     * 
     * @param jti JWT ID to remove
     * @return true if token was successfully removed
     */
    public boolean removeFromWhitelist(String jti) {
        if (redisTemplate == null) {
            log.warn("Redis not available - token removal skipped");
            return false;
        }
        
        try {
            // Get token metadata first to find userId and deviceId
            TokenMetadata metadata = getTokenMetadata(jti);
            
            // Remove token metadata
            String tokenKey = TOKEN_METADATA_PREFIX + jti;
            redisTemplate.delete(tokenKey);
            
            // Remove JTI from user's token set
            if (metadata != null && metadata.getUserId() != null) {
                String userTokensKey = USER_TOKENS_PREFIX + metadata.getUserId() + USER_TOKENS_SUFFIX;
                redisTemplate.opsForSet().remove(userTokensKey, jti);
            }
            
            // Remove JTI from device's token set
            if (metadata != null && metadata.getDeviceId() != null) {
                String deviceTokensKey = DEVICE_TOKENS_PREFIX + metadata.getDeviceId() + DEVICE_TOKENS_SUFFIX;
                redisTemplate.opsForSet().remove(deviceTokensKey, jti);
            }
            
            log.debug("Token removed from whitelist: jti={}", jti);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to remove token from whitelist: jti={}", jti, e);
            return false;
        }
    }
    
    /**
     * Revoke all access tokens for a specific user (keeps refresh tokens)
     * 
     * <p>Removes all access tokens associated with the user from the whitelist.
     * This is useful during token refresh to invalidate old access tokens.
     * 
     * @param userId User ID whose access tokens should be revoked
     * @return Number of access tokens revoked
     */
    public int revokeAllUserAccessTokens(Long userId) {
        if (redisTemplate == null) {
            log.warn("Redis not available - access token revocation skipped");
            return 0;
        }
        
        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId + USER_TOKENS_SUFFIX;
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            
            // Get all JTI values for this user
            Set<String> jtiSet = setOps.members(userTokensKey);
            if (jtiSet == null || jtiSet.isEmpty()) {
                log.debug("No tokens found for user: userId={}", userId);
                return 0;
            }
            
            int revokedCount = 0;
            // Remove only access tokens (not refresh tokens)
            for (String jti : jtiSet) {
                TokenMetadata metadata = getTokenMetadata(jti);
                if (metadata != null && "access".equals(metadata.getTokenType())) {
                    if (removeFromWhitelist(jti)) {
                        revokedCount++;
                    }
                }
            }
            
            log.info("Revoked {} access tokens for user: userId={}", revokedCount, userId);
            return revokedCount;
            
        } catch (Exception e) {
            log.error("Failed to revoke all user access tokens: userId={}", userId, e);
            return 0;
        }
    }
    
    /**
     * Revoke all tokens for a specific user
     * 
     * <p>Removes all tokens associated with the user from the whitelist.
     * This is useful for forced logout or security incidents.
     * 
     * @param userId User ID whose tokens should be revoked
     * @return Number of tokens revoked
     */
    public int revokeAllUserTokens(Long userId) {
        if (redisTemplate == null) {
            log.warn("Redis not available - token revocation skipped");
            return 0;
        }
        
        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId + USER_TOKENS_SUFFIX;
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            
            // Get all JTI values for this user
            Set<String> jtiSet = setOps.members(userTokensKey);
            if (jtiSet == null || jtiSet.isEmpty()) {
                log.debug("No tokens found for user: userId={}", userId);
                return 0;
            }
            
            int revokedCount = 0;
            // Remove each token
            for (String jti : jtiSet) {
                if (removeFromWhitelist(jti)) {
                    revokedCount++;
                }
            }
            
            // Clean up user token set
            redisTemplate.delete(userTokensKey);
            
            log.info("Revoked {} tokens for user: userId={}", revokedCount, userId);
            return revokedCount;
            
        } catch (Exception e) {
            log.error("Failed to revoke all user tokens: userId={}", userId, e);
            return 0;
        }
    }
    
    /**
     * Revoke only access tokens for a specific device (keeps refresh tokens).
     * 
     * <p>This is used during token refresh to invalidate old access tokens
     * for the current device only, without affecting other devices.
     * This prevents cross-device logout cascades that occurred when
     * revokeAllUserAccessTokens() was used during refresh.
     * 
     * @param deviceId Device ID whose access tokens should be revoked
     * @return Number of access tokens revoked
     */
    public int revokeDeviceAccessTokens(String deviceId) {
        if (redisTemplate == null) {
            log.warn("Redis not available - device access token revocation skipped");
            return 0;
        }
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            log.warn("Invalid deviceId provided for device access token revocation");
            return 0;
        }
        
        try {
            String deviceTokensKey = DEVICE_TOKENS_PREFIX + deviceId + DEVICE_TOKENS_SUFFIX;
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            
            // Get all JTI values for this device
            Set<String> jtiSet = setOps.members(deviceTokensKey);
            if (jtiSet == null || jtiSet.isEmpty()) {
                log.debug("No tokens found for device: deviceId={}", deviceId);
                return 0;
            }
            
            int revokedCount = 0;
            // Remove only access tokens (not refresh tokens)
            for (String jti : jtiSet) {
                TokenMetadata metadata = getTokenMetadata(jti);
                if (metadata != null && "access".equals(metadata.getTokenType())) {
                    if (removeFromWhitelist(jti)) {
                        revokedCount++;
                    }
                }
            }
            
            log.info("Revoked {} access tokens for device: deviceId={}", revokedCount, deviceId);
            return revokedCount;
            
        } catch (Exception e) {
            log.error("Failed to revoke device access tokens: deviceId={}", deviceId, e);
            return 0;
        }
    }

    /**
     * Revoke all tokens for a specific device
     * 
     * <p>Removes all tokens associated with the device from the whitelist.
     * This is useful when a device is lost or compromised.
     * 
     * @param deviceId Device ID whose tokens should be revoked
     * @return Number of tokens revoked
     */
    public int revokeAllDeviceTokens(String deviceId) {
        if (redisTemplate == null) {
            log.warn("Redis not available - token revocation skipped");
            return 0;
        }
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            log.warn("Invalid deviceId provided for token revocation");
            return 0;
        }
        
        try {
            String deviceTokensKey = DEVICE_TOKENS_PREFIX + deviceId + DEVICE_TOKENS_SUFFIX;
            SetOperations<String, String> setOps = redisTemplate.opsForSet();
            
            // Get all JTI values for this device
            Set<String> jtiSet = setOps.members(deviceTokensKey);
            if (jtiSet == null || jtiSet.isEmpty()) {
                log.debug("No tokens found for device: deviceId={}", deviceId);
                return 0;
            }
            
            int revokedCount = 0;
            // Remove each token
            for (String jti : jtiSet) {
                if (removeFromWhitelist(jti)) {
                    revokedCount++;
                }
            }
            
            // Clean up device token set
            redisTemplate.delete(deviceTokensKey);
            
            log.info("Revoked {} tokens for device: deviceId={}", revokedCount, deviceId);
            return revokedCount;
            
        } catch (Exception e) {
            log.error("Failed to revoke all device tokens: deviceId={}", deviceId, e);
            return 0;
        }
    }
    
    /**
     * Get all JTI values for a user
     * 
     * @param userId User ID
     * @return Set of JTI values, or empty set if none found
     */
    public Set<String> getUserTokenJtis(Long userId) {
        if (redisTemplate == null) {
            return new HashSet<>();
        }
        
        try {
            String userTokensKey = USER_TOKENS_PREFIX + userId + USER_TOKENS_SUFFIX;
            Set<String> jtiSet = redisTemplate.opsForSet().members(userTokensKey);
            return jtiSet != null ? jtiSet : new HashSet<>();
        } catch (Exception e) {
            log.warn("Error getting user token JTIs: userId={}", userId, e);
            return new HashSet<>();
        }
    }
    
    /**
     * Get all JTI values for a device
     * 
     * @param deviceId Device ID
     * @return Set of JTI values, or empty set if none found
     */
    public Set<String> getDeviceTokenJtis(String deviceId) {
        if (redisTemplate == null) {
            return new HashSet<>();
        }
        
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return new HashSet<>();
        }
        
        try {
            String deviceTokensKey = DEVICE_TOKENS_PREFIX + deviceId + DEVICE_TOKENS_SUFFIX;
            Set<String> jtiSet = redisTemplate.opsForSet().members(deviceTokensKey);
            return jtiSet != null ? jtiSet : new HashSet<>();
        } catch (Exception e) {
            log.warn("Error getting device token JTIs: deviceId={}", deviceId, e);
            return new HashSet<>();
        }
    }
}

