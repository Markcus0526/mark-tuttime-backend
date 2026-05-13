package com.tuttime.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@Order(1)
@ConditionalOnProperty(value = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
public class RedisConfig {

	@Value("${spring.data.redis.host:redis}")
	private String redisHost;

	@Value("${spring.data.redis.port:6379}")
	private int redisPort;

	@Value("${spring.data.redis.password:}")
	private String redisPassword;

	/**
	 * CHOICE B: Manual Connection Factory
	 * This bypasses Spring Boot's failing Sentinel auto-configuration.
	 */
	@Bean
	@Primary
	@ConditionalOnMissingBean(RedisConnectionFactory.class)
	public RedisConnectionFactory redisConnectionFactory() {
		RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
		if (redisPassword != null && !redisPassword.isEmpty()) {
			config.setPassword(redisPassword);
		}
		return new LettuceConnectionFactory(config);
	}

	@Bean(name = "cacheManager")
	@Primary
	@ConditionalOnMissingBean(CacheManager.class)
	public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.findAndRegisterModules();

		GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
				.entryTtl(Duration.ofDays(1))
				.serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
				.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
				.disableCachingNullValues();

		Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

		// --- Your existing cache configurations ---
		cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(1)));
		cacheConfigurations.put("balances", defaultConfig.entryTtl(Duration.ofMinutes(1)));
		cacheConfigurations.put("ap-balances", defaultConfig.entryTtl(Duration.ofMinutes(1)));
		cacheConfigurations.put("status", defaultConfig.entryTtl(Duration.ofMinutes(1)));
		cacheConfigurations.put("status-bonus", defaultConfig.entryTtl(Duration.ofMinutes(1)));
		cacheConfigurations.put("premium-mining-eligibility", defaultConfig.entryTtl(Duration.ofMinutes(1)));
		cacheConfigurations.put("staking-pools", defaultConfig.entryTtl(Duration.ofMinutes(5)));
		cacheConfigurations.put("referral-codes", defaultConfig.entryTtl(Duration.ofMinutes(30)));
		cacheConfigurations.put("video-content", defaultConfig.entryTtl(Duration.ofMinutes(10)));
		cacheConfigurations.put("ads", defaultConfig.entryTtl(Duration.ofMinutes(5)));
		cacheConfigurations.put("map-queries", defaultConfig.entryTtl(Duration.ofMinutes(2)));

		return RedisCacheManager.builder(redisConnectionFactory)
				.cacheDefaults(defaultConfig)
				.withInitialCacheConfigurations(cacheConfigurations)
				.build();
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}
}
