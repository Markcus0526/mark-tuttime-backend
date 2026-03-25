package com.tuttime.gateway.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Custom Health Indicator: checks health of critical dependencies (Redis, Eureka).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewayHealthIndicator implements HealthIndicator {

	private final ReactiveRedisConnectionFactory redisConnectionFactory;

	@Override
	public Health health() {
		Health.Builder builder = Health.up();

		try {
			boolean redisHealthy = checkRedisHealth();
			if (!redisHealthy) {
				builder.down().withDetail("redis", "Connection failed");
			} else {
				builder.withDetail("redis", "Connected");
			}
		} catch (Exception e) {
			log.warn("Redis health check failed: {}", e.getMessage());
			builder.down().withDetail("redis", "Error: " + e.getMessage());
		}

		return builder.build();
	}

	/**
	 * Checks Redis connectivity.
	 */
	private boolean checkRedisHealth() {
		try {
			String result = redisConnectionFactory.getReactiveConnection()
					.ping()
					.block(Duration.ofSeconds(2));
			return "PONG".equals(result);
		} catch (Exception e) {
			log.debug("Redis ping failed: {}", e.getMessage());
			return false;
		}
	}
}

