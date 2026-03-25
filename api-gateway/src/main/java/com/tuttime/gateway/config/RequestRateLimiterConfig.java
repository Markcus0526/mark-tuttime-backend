package com.tuttime.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Request Rate Limiter Configuration
 *
 * <p>Configures KeyResolver for Spring Cloud Gateway's RequestRateLimiter filter.
 * The KeyResolver determines the key used for rate limiting (e.g., IP address, user ID).
 *
 * <p>For admin routes, we use IP-based rate limiting to prevent abuse.
 */
@Configuration
public class RequestRateLimiterConfig {

	/**
	 * IP-based KeyResolver for rate limiting
	 *
	 * <p>Uses the client's IP address as the rate limit key.
	 * This ensures rate limiting is applied per IP address.
	 *
	 * <p>IP extraction priority:
	 * <ol>
	 *   <li>X-Forwarded-For header (first IP if multiple)</li>
	 *   <li>X-Real-IP header</li>
	 *   <li>Remote address from request</li>
	 * </ol>
	 *
	 * <p>This bean is used by Spring Cloud Gateway's RequestRateLimiter filter.
	 * The default bean name "keyResolver" is used automatically.
	 *
	 * @return KeyResolver that extracts IP address
	 */
	@Bean(name = "keyResolver")
	public KeyResolver keyResolver() {
		return exchange -> {
			String ipAddress = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");

			if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
				ipAddress = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
			}

			if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
				ipAddress = Objects.requireNonNull(exchange.getRequest().getRemoteAddress())
						.getAddress()
						.getHostAddress();
			}

			// If X-Forwarded-For contains multiple IPs, take the first one
			if (ipAddress != null && ipAddress.contains(",")) {
				ipAddress = ipAddress.split(",")[0].trim();
			}

			return Mono.just(ipAddress != null ? ipAddress : "unknown");
		};
	}
}

