package com.tuttime.common.warmup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class BaseApplicationWarmup {

	/**
	 * Warms up database repositories (non‑blocking, one attempt).
	 */
	protected void warmUpDatabase(JpaRepository<?,?>... repos) {
		for (var repo : repos) {
			try {
				repo.count();
				log.debug("Warmed up repository: {}", repo.getClass().getSimpleName());
			} catch (Exception e) {
				log.debug("Warmup failed for repository {}: {}", repo.getClass().getSimpleName(), e.getMessage());
			}
		}
	}

	/**
	 * Warms up Redis (non‑blocking, one attempt).
	 */
	protected void warmUpRedis(StringRedisTemplate redis) {
		try {
			redis.opsForValue().get("warmup");
			log.debug("Redis warmup successful");
		} catch (Exception e) {
			log.debug("Redis warmup failed: {}", e.getMessage());
		}
	}

	/**
	 * Warms up gRPC or HTTP calls asynchronously with infinite retries and exponential backoff.
	 * @param warmupCalls runnables that perform the warmup (e.g., stub.ping())
	 */
	protected void warmUpAsync(Runnable... warmupCalls) {
		for (Runnable call : warmupCalls) {
			CompletableFuture.runAsync(() -> {
				int attempt = 1;
				long delayMs = 30000;
				int maxAttempts = Integer.MAX_VALUE; // keep retrying forever
				while (attempt <= maxAttempts) {
					try {
						call.run();
						log.debug("[Warmup] call succeeded for {} after {} attempt(s)", call, attempt);
						return;
					} catch (Exception e) {
						if (attempt % 10 == 0) {
							log.debug("Warmup call still failing after {} attempts: {}", attempt, e.getMessage());
						}
						try {
							TimeUnit.MILLISECONDS.sleep(delayMs);
						} catch (InterruptedException ie) {
							Thread.currentThread().interrupt();
							return;
						}
						delayMs = Math.min(delayMs * 2, 120000); // cap at 120 seconds
						attempt++;
					}
				}
			});
		}
	}
}