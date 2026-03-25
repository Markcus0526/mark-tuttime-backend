package com.tuttime.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache Configuration
 *
 * <p>Configures Redis caching for admin service.
 * Critical for 10M user scale to reduce database and service call load.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		// Configure ObjectMapper with Java 8 time module for Redis serialization
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
		// Configure to include type information for proper deserialization
		// This ensures cached objects are deserialized to their correct types instead of LinkedHashMap
		// Using PROPERTY format (@class property) instead of WRAPPER_ARRAY for better compatibility
		// Note: Old cached entries may need to be cleared if they were stored with WRAPPER_ARRAY format
		objectMapper.activateDefaultTyping(
				objectMapper.getPolymorphicTypeValidator(),
				com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,
				com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);

		GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofMinutes(5)) // Default 5 minutes TTL
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
						new StringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
						jsonSerializer))
				.disableCachingNullValues();

		// User details cache - 10 minutes TTL (frequently accessed)
		RedisCacheConfiguration userDetailsConfig = defaultConfig.entryTtl(Duration.ofMinutes(10));

		// Analytics cache - 5 minutes TTL (reduced from 15 to ensure fresher data)
		// Note: Dashboard stats are cached to reduce load, but shorter TTL ensures data is more current
		RedisCacheConfiguration analyticsConfig = defaultConfig.entryTtl(Duration.ofMinutes(5));

		return RedisCacheManager.builder(redisConnectionFactory)
				.cacheDefaults(defaultConfig)
				.withCacheConfiguration("userDetails", userDetailsConfig)
				.withCacheConfiguration("analytics", analyticsConfig)
				.transactionAware()
				.build();
	}
}

