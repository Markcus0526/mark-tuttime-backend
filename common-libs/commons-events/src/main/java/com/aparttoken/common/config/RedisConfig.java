package com.aparttoken.common.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


/**
 * Redis Cache Configuration
 *
 * Note: IntelliJ may show a warning about RedisConnectionFactory not being found.
 * This is a false positive - the bean is created by Spring Boot auto-configuration
 * at runtime when Redis properties are configured in application.yml.
 *
 * The @ConditionalOnBean ensures this config only loads when RedisConnectionFactory exists.
 */
@Configuration
@EnableCaching
@Order(1) // Ensure this configuration loads before Spring Boot's auto-configuration
@ConditionalOnProperty(value = "spring.cache.type", havingValue = "redis", matchIfMissing = true)
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisConfig {

    @Bean(name = "cacheManager")
    @Primary
    @ConditionalOnMissingBean(CacheManager.class) // Only create if no other CacheManager exists
    @SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringJavaAutowiringInspection"})
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Configure ObjectMapper for proper JSON serialization with Java 8 time support
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

        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(1)));

        // Balance cache - 1 minute TTL (frequently updated)
        cacheConfigurations.put("balances", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // AP balance cache - 1 minute TTL
        cacheConfigurations.put("ap-balances", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Status cache - 1 minute TTL (status changes when balance changes)
        cacheConfigurations.put("status", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Status bonus cache - 1 minute TTL (same as status)
        cacheConfigurations.put("status-bonus", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Premium mining eligibility cache - 1 minute TTL (same as status)
        cacheConfigurations.put("premium-mining-eligibility", defaultConfig.entryTtl(Duration.ofMinutes(1)));

        // Staking pools cache - 5 minutes TTL (rarely change)
        cacheConfigurations.put("staking-pools", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Referral codes cache - 30 minutes TTL
        cacheConfigurations.put("referral-codes", defaultConfig.entryTtl(Duration.ofMinutes(30)));

        // Video content cache - 10 minutes TTL
        cacheConfigurations.put("video-content", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        // Ads cache - 5 minutes TTL
        cacheConfigurations.put("ads", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        // Map queries cache - 2 minutes TTL (location data changes frequently)
        // Cache keys include bounds/center/nearby parameters for precise cache hits
        cacheConfigurations.put("map-queries", defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
