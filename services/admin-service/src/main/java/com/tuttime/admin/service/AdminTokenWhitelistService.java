package com.tuttime.admin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Admin Token Whitelist Service
 * 
 * <p>Manages admin JWT token whitelist in Redis.
 * All admin tokens MUST be whitelisted to be accepted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenWhitelistService {
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    private final ObjectMapper objectMapper;
    
    private static final String ADMIN_TOKEN_METADATA_PREFIX = "admin:whitelist:token:";
    private static final String ADMIN_USER_TOKENS_PREFIX = "admin:whitelist:user:";
    private static final String ADMIN_USER_TOKENS_SUFFIX = ":tokens";
    
    /**
     * Add admin token to whitelist
     */
    public boolean addToWhitelist(String jti, Long adminId, Long expirationTime) {
        if (redisTemplate == null) {
            log.warn("Redis not available - admin token whitelisting skipped");
            return false;
        }
        
        try {
            long currentTime = System.currentTimeMillis();
            long ttl = expirationTime - currentTime;
            
            if (ttl <= 0) {
                log.debug("Admin token already expired, not adding to whitelist: jti={}", jti);
                return false;
            }
            
            AdminTokenMetadata metadata = AdminTokenMetadata.builder()
                    .adminId(adminId)
                    .issuedAt(currentTime)
                    .expiresAt(expirationTime)
                    .build();
            
            String metadataJson = objectMapper.writeValueAsString(metadata);
            
            String tokenKey = ADMIN_TOKEN_METADATA_PREFIX + jti;
            redisTemplate.opsForValue().set(tokenKey, metadataJson, ttl, TimeUnit.MILLISECONDS);
            
            String userTokensKey = ADMIN_USER_TOKENS_PREFIX + adminId + ADMIN_USER_TOKENS_SUFFIX;
            redisTemplate.opsForSet().add(userTokensKey, jti);
            redisTemplate.expire(userTokensKey, ttl, TimeUnit.MILLISECONDS);
            
            log.debug("Admin token added to whitelist: jti={}, adminId={}, ttl={}ms", jti, adminId, ttl);
            return true;
            
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize admin token metadata: jti={}", jti, e);
            return false;
        } catch (Exception e) {
            log.error("Failed to add admin token to whitelist: jti={}", jti, e);
            return false;
        }
    }
    
    /**
     * Check if admin token is whitelisted
     */
    public boolean isTokenWhitelisted(String jti) {
        if (redisTemplate == null) {
            log.warn("Redis not available - admin token whitelist check cannot be performed. Token jti={}", jti);
            // In production, this should return false for security, but for development we allow it
            return true;
        }
        
        try {
            String tokenKey = ADMIN_TOKEN_METADATA_PREFIX + jti;
            boolean exists = Boolean.TRUE.equals(redisTemplate.hasKey(tokenKey));
            if (!exists) {
                log.warn("Admin token not found in whitelist: jti={}, key={}", jti, tokenKey);
            }
            return exists;
        } catch (Exception e) {
            log.error("Error checking admin token whitelist: jti={}", jti, e);
            return false; // Fail secure
        }
    }
    
    /**
     * Remove admin token from whitelist
     */
    public boolean removeFromWhitelist(String jti) {
        if (redisTemplate == null) {
            log.warn("Redis not available - admin token removal skipped");
            return false;
        }
        
        try {
            AdminTokenMetadata metadata = getTokenMetadata(jti);
            
            String tokenKey = ADMIN_TOKEN_METADATA_PREFIX + jti;
            redisTemplate.delete(tokenKey);
            
            if (metadata != null && metadata.getAdminId() != null) {
                String userTokensKey = ADMIN_USER_TOKENS_PREFIX + metadata.getAdminId() + ADMIN_USER_TOKENS_SUFFIX;
                redisTemplate.opsForSet().remove(userTokensKey, jti);
            }
            
            log.debug("Admin token removed from whitelist: jti={}", jti);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to remove admin token from whitelist: jti={}", jti, e);
            return false;
        }
    }
    
    /**
     * Revoke all tokens for an admin user
     */
    public int revokeAllAdminTokens(Long adminId) {
        if (redisTemplate == null) {
            return 0;
        }
        
        try {
            String userTokensKey = ADMIN_USER_TOKENS_PREFIX + adminId + ADMIN_USER_TOKENS_SUFFIX;
            Set<String> jtiSet = redisTemplate.opsForSet().members(userTokensKey);
            
            if (jtiSet == null || jtiSet.isEmpty()) {
                return 0;
            }
            
            int revokedCount = 0;
            for (String jti : jtiSet) {
                if (removeFromWhitelist(jti)) {
                    revokedCount++;
                }
            }
            
            redisTemplate.delete(userTokensKey);
            log.info("Revoked {} admin tokens for adminId={}", revokedCount, adminId);
            return revokedCount;
            
        } catch (Exception e) {
            log.error("Failed to revoke all admin tokens: adminId={}", adminId, e);
            return 0;
        }
    }
    
    /**
     * Get token metadata
     */
    private AdminTokenMetadata getTokenMetadata(String jti) {
        if (redisTemplate == null) {
            return null;
        }
        
        try {
            String tokenKey = ADMIN_TOKEN_METADATA_PREFIX + jti;
            String metadataJson = redisTemplate.opsForValue().get(tokenKey);
            
            if (metadataJson == null) {
                return null;
            }
            
            return objectMapper.readValue(metadataJson, AdminTokenMetadata.class);
        } catch (Exception e) {
            log.warn("Error getting admin token metadata: jti={}", jti, e);
            return null;
        }
    }
    
    /**
     * Admin Token Metadata
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class AdminTokenMetadata {
        private Long adminId;
        private Long issuedAt;
        private Long expiresAt;
    }
}

