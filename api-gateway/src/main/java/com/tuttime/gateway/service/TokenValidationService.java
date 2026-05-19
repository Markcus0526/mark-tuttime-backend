package com.tuttime.gateway.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenValidationService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final String WHITELIST_PREFIX = "whitelist:token:";
    private static final String BLACKLIST_PREFIX = "blacklist:jti:";   // matches Kafka consumer
    private static final String USER_BLOCK_PREFIX = "blocklist:user:";

    /**
     * Checks if a token JTI is whitelisted (fail‑closed).
     */
    public Mono<Boolean> isTokenWhitelisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return Mono.just(false);
        }
        String key = WHITELIST_PREFIX + jti;
        return reactiveRedisTemplate.hasKey(key)
                .defaultIfEmpty(false)
                .doOnError(e -> log.error("Whitelist check error, rejecting token (fail‑closed): jti={}", jti, e))
                .onErrorReturn(false);
    }

    /**
     * Checks if a token JTI is blacklisted (fail‑closed).
     * Uses the same key prefix as the Kafka consumer.
     */
    public Mono<Boolean> isTokenBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            return Mono.just(false);
        }
        String key = BLACKLIST_PREFIX + jti;
        return reactiveRedisTemplate.hasKey(key)
                .defaultIfEmpty(false)
                .doOnError(e -> log.error("Blacklist check error, assuming blacklisted (fail‑closed): jti={}", jti, e))
                .onErrorReturn(true);   // fail‑closed: assume blacklisted
    }

    /**
     * Checks if a user is blocked (fail‑closed).
     */
    public Mono<Boolean> isUserBlocked(UUID userId) {
        if (userId == null) {
            return Mono.just(false);
        }
        String key = USER_BLOCK_PREFIX + userId.toString();
        return reactiveRedisTemplate.hasKey(key)
                .defaultIfEmpty(false)
                .doOnError(e -> log.error("User block check error, assuming blocked (fail‑closed): userId={}", userId, e))
                .onErrorReturn(true);
    }
}