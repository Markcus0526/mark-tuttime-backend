package com.tuttime.booking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTableLockService {
	private final RedisTemplate<String, String> redisTemplate;

	public boolean lockTable(Long tableId, String bookingId, Duration ttl) {
		String key = "table-lock:" + tableId;
		long ttlMillis = ttl.toMillis();
		Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, bookingId, ttlMillis, TimeUnit.MILLISECONDS);
		log.info("Table {} locked for {}", tableId, bookingId);
		return locked != null && locked;
	}

	public boolean extendLock(Long tableId, String bookingId, Duration ttl) {
		String key = "table-lock:" + tableId;
		long ttlMillis = ttl.toMillis();
		Boolean extended = redisTemplate.expire(key, ttlMillis, TimeUnit.MILLISECONDS);
		log.info("Table {} lock extended for {}", tableId, bookingId);
		return extended != null && extended;
	}

	public void releaseLock(Long tableId) {
		String key = "table-lock:" + tableId;
		Boolean isDeleted = redisTemplate.delete(key);
		log.info("Table {} lock released: {}", tableId, Boolean.TRUE.equals(isDeleted) ? "success" : "no lock found");
	}

	public boolean isLocked(Long tableId) {
		String key = "table-lock:" + tableId;
		String value = (String) redisTemplate.opsForValue().get(key);
		return value != null;
	}
}
