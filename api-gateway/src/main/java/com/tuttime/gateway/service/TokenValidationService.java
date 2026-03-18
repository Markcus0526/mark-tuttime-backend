package com.tuttime.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/** Service for validating token whitelist and blacklist status using Redis. Uses same key structure as user-service. */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenValidationService {
    
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private static final String TOKEN_METADATA_PREFIX = "whitelist:token:";
    private static final String TOKEN_BLACKLIST_PREFIX = "blacklist:token:";
    
    /** Checks if token is whitelisted. Returns false on errors (fail secure). */
    public Mono<Boolean> isTokenWhitelisted(String jti) {
        if (jti == null || jti.trim().isEmpty()) {
            return Mono.just(false);
        }
        
        try {
            String tokenKey = TOKEN_METADATA_PREFIX + jti;
            // Remove retry logic to reduce latency - Redis replication lag should be minimal
            // If token is not found, it's likely not whitelisted rather than replication lag
            return reactiveRedisTemplate.hasKey(tokenKey)
                    .defaultIfEmpty(false)
                    .doOnError(error -> {
                        log.error("Error checking token whitelist: jti={}, error={}", jti, error.getMessage());
                    })
                    .onErrorReturn(false); // On error, assume NOT whitelisted (fail secure)
        } catch (Exception e) {
            log.error("Exception checking token whitelist: jti={}", jti, e);
            return Mono.just(false); // Fail secure - only accept if confirmed whitelisted
        }
    }
    
    /** Check if a token is blacklisted. */
    public Mono<Boolean> isTokenBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Mono.just(false);
        }
        
        try {
            String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
            return reactiveRedisTemplate.hasKey(blacklistKey)
                    .defaultIfEmpty(false)
                    .doOnError(error -> {
                        log.warn("Error checking token blacklist: error={}", error.getMessage());
                    })
                    .onErrorReturn(false); // On error, assume not blacklisted (fail secure)
        } catch (Exception e) {
            log.warn("Exception checking token blacklist", e);
            return Mono.just(false); // Fail secure
        }
    }
}

