package com.tuttime.user.config;

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
 * Cache Configuration for User Service
 * 
 * <p>Configures Redis caching with proper type information to prevent LinkedHashMap
 * deserialization issues. This ensures cached objects are deserialized to their
 * correct types (e.g., UserResponse, ReferralInfoResponse) instead of LinkedHashMap.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,
                com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY);
        
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        jsonSerializer))
                .disableCachingNullValues();

        RedisCacheConfiguration usersConfig = defaultConfig.entryTtl(Duration.ofHours(1));
        RedisCacheConfiguration referralInfoConfig = defaultConfig.entryTtl(Duration.ofMinutes(30));
        RedisCacheConfiguration imageUrlsConfig = defaultConfig.entryTtl(Duration.ofDays(1)); // 1 day TTL for image URLs
        RedisCacheConfiguration appVersionConfig = defaultConfig.entryTtl(Duration.ofMinutes(5)); // 5 minutes TTL for app version

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withCacheConfiguration("users", usersConfig)
                .withCacheConfiguration("referral-info", referralInfoConfig)
                .withCacheConfiguration("referral-info-rest", referralInfoConfig)
                .withCacheConfiguration("image-urls", imageUrlsConfig) // Cache image URLs for 1 day
                .withCacheConfiguration("appVersion", appVersionConfig) // Cache app version for 5 minutes
                .transactionAware()
                .build();
    }
}

