package com.tuttime.common.warmup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;

public abstract class BaseApplicationWarmup {
	protected void warmUpDatabase(JpaRepository<?,?>... repos) {
		for (var repo : repos) {
			repo.count();
		}
	}
	protected void warmUpRedis(StringRedisTemplate redis) {
		redis.opsForValue().get("warmup");
	}
	protected void warmUpGrpc(Runnable... warmupCalls) {
		for (Runnable call : warmupCalls) {
			try { call.run(); } catch (Exception ignored) {}
		}
	}
}