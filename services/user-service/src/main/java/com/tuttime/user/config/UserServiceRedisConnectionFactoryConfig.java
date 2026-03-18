package com.tuttime.user.config;

import io.lettuce.core.ClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Redis Connection Factory Configuration for User Service
 * 
 * <p>Handles both Sentinel and direct connection modes.
 * Uses direct connection when Sentinel is not configured.
 * 
 * <p>This configuration is only active when Redis cache is enabled.
 * 
 * <p>Note: This class is renamed from RedisConfig to avoid conflict with
 * {@link com.tuttime.common.config.RedisConfig} which provides the CacheManager.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis", matchIfMissing = false)
public class UserServiceRedisConnectionFactoryConfig {

    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Value("${spring.data.redis.host:apart-token-redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.timeout:5000ms}")
    private Duration timeout;

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory(RedisProperties redisProperties) {
        LettuceConnectionFactory factory;

        // Connection mode selection:
        // - Kubernetes: Sentinel is configured (SPRING_DATA_REDIS_SENTINEL_MASTER and SPRING_DATA_REDIS_SENTINEL_NODES are set)
        //   → Uses Redis Sentinel for high availability (no direct host connection)
        // - Docker/Local: Sentinel is NOT configured (both master and nodes are empty)
        //   → Uses direct Redis connection (host:port)
        // This ensures proper Redis connection strategy for each environment.
        if (StringUtils.hasText(sentinelMaster) && StringUtils.hasText(sentinelNodes)) {
            // Sentinel mode
            RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
            sentinelConfig.setMaster(sentinelMaster);
            
            List<String> nodes = Arrays.asList(sentinelNodes.split(","));
            for (String node : nodes) {
                String[] hostPort = node.trim().split(":");
                if (hostPort.length == 2) {
                    sentinelConfig.sentinel(hostPort[0], Integer.parseInt(hostPort[1]));
                }
            }
            
            if (StringUtils.hasText(redisPassword)) {
                sentinelConfig.setPassword(redisPassword);
            }

            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(timeout)
                    .clientOptions(ClientOptions.builder()
                            .autoReconnect(true)
                            .pingBeforeActivateConnection(true)
                            .build())
                    .build();

            factory = new LettuceConnectionFactory(sentinelConfig, clientConfig);
            log.info("Using Redis Sentinel for user service. Master: {}, Nodes: {}", 
                    sentinelMaster, sentinelNodes);
        } else {
            // Direct connection mode (default for Docker/local development)
            org.springframework.data.redis.connection.RedisStandaloneConfiguration standaloneConfig = 
                    new org.springframework.data.redis.connection.RedisStandaloneConfiguration();
            standaloneConfig.setHostName(redisHost);
            standaloneConfig.setPort(redisPort);
            
            if (StringUtils.hasText(redisPassword)) {
                standaloneConfig.setPassword(redisPassword);
            }

            LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                    .commandTimeout(timeout)
                    .clientOptions(ClientOptions.builder()
                            .autoReconnect(true)
                            .pingBeforeActivateConnection(true)
                            .build())
                    .build();

            factory = new LettuceConnectionFactory(standaloneConfig, clientConfig);
            log.info("Using direct Redis connection for user service. Host: {}, Port: {}", 
                    redisHost, redisPort);
        }

        factory.afterPropertiesSet();
        return factory;
    }
}

