package com.tuttime.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TokenWhitelistService {
	private final StringRedisTemplate redisTemplate;
	private static final String WHITELIST_PREFIX = "whitelist:token:";

	public void whitelist(String jti, long ttlSeconds) {
		if (ttlSeconds > 0) {
			redisTemplate.opsForValue().set(WHITELIST_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
		}
	}

	public void remove(String jti) {
		redisTemplate.delete(WHITELIST_PREFIX + jti);
	}
}