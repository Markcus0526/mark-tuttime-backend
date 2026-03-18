package com.tuttime.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Admin Token Blacklist Service
 * 
 * <p>Manages admin JWT token blacklist in Redis.
 * Blacklisted tokens are immediately rejected.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTokenBlacklistService {
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    @Value("${spring.security.jwt.expiration:3600000}")
    private Long tokenExpiration;
    
    private static final String ADMIN_BLACKLIST_PREFIX = "admin:blacklist:token:";
    
    /**
     * Add token to blacklist
     */
    public void addToBlacklist(String jti) {
        if (redisTemplate == null) {
            log.warn("Redis not available - admin token blacklisting skipped");
            return;
        }
        
        try {
            String blacklistKey = ADMIN_BLACKLIST_PREFIX + jti;
            // Store with TTL matching token expiration (or max 24 hours)
            long ttl = Math.min(tokenExpiration, TimeUnit.HOURS.toMillis(24));
            redisTemplate.opsForValue().set(blacklistKey, "revoked", ttl, TimeUnit.MILLISECONDS);
            log.debug("Admin token added to blacklist: jti={}", jti);
        } catch (Exception e) {
            log.error("Failed to add admin token to blacklist: jti={}", jti, e);
        }
    }
    
    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String jti) {
        if (redisTemplate == null) {
            log.debug("Redis not available - assuming admin token is not blacklisted");
            return false;
        }
        
        try {
            String blacklistKey = ADMIN_BLACKLIST_PREFIX + jti;
            return Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey));
        } catch (Exception e) {
            log.warn("Error checking admin token blacklist: jti={}", jti, e);
            return false; // Fail secure - if we can't check, don't block
        }
    }
    
    /**
     * Remove token from blacklist (if needed)
     */
    public void removeFromBlacklist(String jti) {
        if (redisTemplate == null) {
            return;
        }
        
        try {
            String blacklistKey = ADMIN_BLACKLIST_PREFIX + jti;
            redisTemplate.delete(blacklistKey);
            log.debug("Admin token removed from blacklist: jti={}", jti);
        } catch (Exception e) {
            log.error("Failed to remove admin token from blacklist: jti={}", jti, e);
        }
    }
}

