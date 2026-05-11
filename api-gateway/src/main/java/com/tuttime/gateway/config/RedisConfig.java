package com.tuttime.gateway.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import reactor.core.publisher.Mono;

/**
 * Redis Configuration for API Gateway
 *
 * <p>This configuration creates a ReactiveRedisTemplate<String, String> bean
 * that is used for token whitelist and blacklist validation in the gateway.
 *
 * <p>The reactive template is used because Spring Cloud Gateway is built on
 * reactive programming (WebFlux).
 */
@Configuration("apiGatewayRedisConfig")
public class RedisConfig {

	@Bean
	public ReactiveRedisTemplate<String, String> reactiveRedisTemplate(
			ReactiveRedisConnectionFactory connectionFactory) {
		StringRedisSerializer serializer = new StringRedisSerializer();

		RedisSerializationContext.RedisSerializationContextBuilder<String, String> builder =
				RedisSerializationContext.newSerializationContext(serializer);

		RedisSerializationContext<String, String> context = builder
				.key(serializer)
				.value(serializer)
				.hashKey(serializer)
				.hashValue(serializer)
				.build();

		return new ReactiveRedisTemplate<>(connectionFactory, context);
	}

	/**
	 * ObjectMapper bean for JSON serialization/deserialization.
	 * Used by GlobalExceptionHandler and other components.
	 */
	@Bean
	public ObjectMapper objectMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
		return mapper;
	}

	/**
	 * KeyResolver that uses the authenticated user ID if present,
	 * otherwise falls back to the client IP address.
	 */
	@Bean
	public KeyResolver userKeyResolver() {
		return exchange -> {
			// Try to get user ID from the JWT filter headers
			String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
			if (userId != null && !userId.isEmpty()) {
				return Mono.just(userId);
			}
			// Fallback to client IP
			String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
			return Mono.just(ip);
		};
	}

}

