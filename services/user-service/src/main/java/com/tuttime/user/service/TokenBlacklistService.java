package com.tuttime.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:jti:";

    /**
     * Adds JTI to blacklist with an expiry.
     *
     * @param jti              The unique JWT ID.
     * @param expirationMillis Remaining time until the access token expires.
     */
    public void blacklistJti(String jti, long expirationMillis) {
        if (expirationMillis > 0) {
            redisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + jti,
                    "revoked",
                    expirationMillis,
                    TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * Checks if a JTI is blacklisted.
     */
    public boolean isBlacklisted(String jti) {
        return redisTemplate.hasKey(BLACKLIST_PREFIX + jti);
    }
}
