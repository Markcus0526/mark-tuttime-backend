package com.tuttime.bookingservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisTableLockService {
  private final RedisTemplate<String, String> redisTemplate;

  public boolean lockTable(Long tableId, String bookingId, Duration ttl) {
    String key = "table-lock:" + tableId;
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, bookingId, ttl);
    log.info("Table {} locked for {}: {}", tableId, bookingId, locked);
    return locked != null && locked;
  }

  public boolean extendLock(Long tableId, String bookingId, Duration ttl) {
    String key = "table-lock:" + tableId;
    Boolean extended = redisTemplate.expire(key, ttl);
    log.info("Table {} lock extended for {}: {}", tableId, bookingId, extended);
    return extended != null && extended;
  }

  public void releaseLock(Long tableId) {
    String key = "table-lock:" + tableId;
    Long deleted = redisTemplate.delete(key);
    log.info("Table {} lock released: {}", tableId, deleted > 0);
  }

  public boolean isLocked(Long tableId) {
    String key = "table-lock:" + tableId;
    String value = (String) redisTemplate.opsForValue().get(key);
    return value != null;
  }
}
