package com.tuttime.analytics.config; // Adjust package to match your folder tree

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class RedisConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private List<String> sentinelNodes;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConfiguration config;

        if (sentinelMaster != null && !sentinelMaster.isBlank() && sentinelNodes != null && !sentinelNodes.isEmpty()) {
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
            sentinelConfig.master(sentinelMaster);

            sentinelNodes.forEach(node -> {
                String[] parts = node.split(":");
                sentinelConfig.sentinel(parts[0], Integer.parseInt(parts[1]));
            });

            if (redisPassword != null && !redisPassword.isBlank()) {
                sentinelConfig.setPassword(redisPassword);
            }
            config = sentinelConfig;

        } else {
            RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration(redisHost, redisPort);
            if (redisPassword != null && !redisPassword.isBlank()) {
                standaloneConfig.setPassword(redisPassword);
            }
            config = standaloneConfig;
        }

        return new LettuceConnectionFactory(config);
    }

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * Configures the production Redis Cache Manager. This links your custom method evictions
     * straight to the shared cluster data store, fixing the stop-list syncing bug!
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(2)) // Default 2-hour expiration safety fence
                .disableCachingNullValues();

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
