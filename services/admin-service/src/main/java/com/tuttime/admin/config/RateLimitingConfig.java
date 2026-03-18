package com.tuttime.admin.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Admin Rate Limiting Configuration
 * 
 * <p>Configures rate limiting for admin endpoints to prevent brute-force attacks.
 * Uses Bucket4j with Redis for distributed rate limiting.
 */
@Slf4j
@Configuration
public class RateLimitingConfig {

    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    @Value("${spring.data.redis.host:tuttime-redis}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Creates a Redis client for Bucket4j rate limiting.
     * 
     * <p>Connection mode selection:
     * - Kubernetes: Sentinel is configured (SPRING_DATA_REDIS_SENTINEL_MASTER and SPRING_DATA_REDIS_SENTINEL_NODES are set)
     *   → Uses Redis Sentinel for high availability (no direct host connection)
     * - Docker/Local: Sentinel is NOT configured (both master and nodes are empty)
     *   → Uses direct Redis connection (host:port)
     * This ensures proper Redis connection strategy for each environment.
     */
    @Bean
    public RedisClient adminRedisClient() {
        RedisURI.Builder uriBuilder;
        
        // Connection mode selection:
        // - Kubernetes: Sentinel is configured (SPRING_DATA_REDIS_SENTINEL_MASTER and SPRING_DATA_REDIS_SENTINEL_NODES are set)
        //   → Uses Redis Sentinel for high availability (no direct host connection)
        // - Docker/Local: Sentinel is NOT configured (both master and nodes are empty)
        //   → Uses direct Redis connection (host:port)
        // This ensures proper Redis connection strategy for each environment.
        if (sentinelMaster != null && !sentinelMaster.trim().isEmpty() && 
            sentinelNodes != null && !sentinelNodes.trim().isEmpty()) {
            // Sentinel mode
            uriBuilder = RedisURI.builder()
                    .withSentinelMasterId(sentinelMaster);
            
            for (String node : sentinelNodes.split(",")) {
                String[] hostPort = node.trim().split(":");
                if (hostPort.length == 2) {
                    uriBuilder.withSentinel(hostPort[0], Integer.parseInt(hostPort[1]));
                }
            }
            
            log.info("Using Redis Sentinel for admin rate limiting. Master: {}, Nodes: {}", 
                    sentinelMaster, sentinelNodes);
        } else {
            // Direct connection mode (default for Docker/local development)
            uriBuilder = RedisURI.builder()
                    .withHost(redisHost)
                    .withPort(redisPort);
            
            log.info("Using direct Redis connection for admin rate limiting. Host: {}, Port: {}", 
                    redisHost, redisPort);
        }

        if (redisPassword != null && !redisPassword.isEmpty()) {
            uriBuilder.withPassword(redisPassword.toCharArray());
        }

        // Set connection timeout (10 seconds)
        uriBuilder.withTimeout(Duration.ofSeconds(10));

        RedisURI redisUri = uriBuilder.build();
        RedisClient client = RedisClient.create(redisUri);
        
        // Enable TCP keepalive & auto-reconnect for production
        client.setOptions(ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .build());

        return client;
    }

    /**
     * Creates a Redis connection for Bucket4j proxy manager with retry logic.
     */
    @Bean
    public StatefulRedisConnection<byte[], byte[]> adminRedisConnection(RedisClient adminRedisClient) {
        boolean useSentinel = sentinelMaster != null && !sentinelMaster.trim().isEmpty() && 
                              sentinelNodes != null && !sentinelNodes.trim().isEmpty();
        int maxRetries = 10;
        long initialDelayMs = 1000; // Start with 1 second
        long maxDelayMs = 10000; // Max 10 seconds between retries
        long totalTimeoutMs = 120000; // Total timeout: 2 minutes
        
        long startTime = System.currentTimeMillis();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                StatefulRedisConnection<byte[], byte[]> connection = adminRedisClient.connect(ByteArrayCodec.INSTANCE);
                if (useSentinel) {
                    log.info("Successfully connected to Redis Sentinel for admin rate limiting on attempt {}. Master: {}, Nodes: {}", 
                            attempt, sentinelMaster, sentinelNodes);
                } else {
                    log.info("Successfully connected to Redis for admin rate limiting on attempt {}. Host: {}, Port: {}", 
                            attempt, redisHost, redisPort);
                }
                return connection;
            } catch (Exception e) {
                lastException = e;
                long elapsedTime = System.currentTimeMillis() - startTime;
                
                if (elapsedTime >= totalTimeoutMs) {
                    log.error("Failed to connect to Redis for admin rate limiting after {}ms (timeout exceeded). Last error: {}", 
                            elapsedTime, e.getMessage());
                    break;
                }
                
                if (attempt < maxRetries) {
                    // Exponential backoff with jitter
                    long delayMs = Math.min(initialDelayMs * (1L << (attempt - 1)), maxDelayMs);
                    // Add jitter: ±20% of delay
                    long jitter = (long) (delayMs * 0.2 * (Math.random() * 2 - 1));
                    delayMs = Math.max(100, delayMs + jitter); // Minimum 100ms
                    
                    log.warn("Failed to connect to Redis for admin rate limiting on attempt {}/{}. Retrying in {}ms. Error: {}", 
                            attempt, maxRetries, delayMs, e.getMessage());
                    
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted while waiting to retry Redis connection", ie);
                    }
                } else {
                    log.error("Failed to connect to Redis for admin rate limiting after {} attempts. Last error: {}", 
                            maxRetries, e.getMessage());
                }
            }
        }
        
        // If we get here, all retries failed
        if (sentinelMaster != null && !sentinelMaster.trim().isEmpty()) {
            log.error("Please ensure Redis Sentinel is running and accessible. Master: {}, Nodes: {}", 
                    sentinelMaster, sentinelNodes);
        } else {
            log.error("Please ensure Redis is running and accessible. Host: {}, Port: {}", 
                    redisHost, redisPort);
        }
        throw new RuntimeException("Failed to connect to Redis for admin rate limiting after " + maxRetries + 
                " attempts. Please check Redis configuration and ensure it is running.", lastException);
    }

    @Bean
    public LettuceBasedProxyManager<byte[]> adminProxyManager(StatefulRedisConnection<byte[], byte[]> connection) {
        LettuceBasedProxyManager<byte[]> proxyManager = LettuceBasedProxyManager.builderFor(connection)
                .withExpirationStrategy(ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(Duration.ofHours(24)))
                .build();
        
        log.info("Created Bucket4j proxy manager for admin rate limiting");
        return proxyManager;
    }

    /**
     * Rate limit configurations for admin endpoints
     * 
     * <p>Brute-force protection:
     * <ul>
     *   <li>Login: 5 attempts per 15 minutes per IP</li>
     *   <li>General admin endpoints: 100 requests/minute per admin</li>
     * </ul>
     */
    @Bean
    public Map<String, Supplier<Bucket>> adminRateLimitConfigurations(LettuceBasedProxyManager<byte[]> proxyManager) {
        Map<String, Supplier<Bucket>> configurations = new HashMap<>();

        // Admin login: 5 attempts per 15 minutes per IP (brute-force protection)
        configurations.put("/api/admin/auth/login", () -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(5)
                    .refillIntervally(5, Duration.ofMinutes(15))
                    .build();
            BucketConfiguration bucketConfig = BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
            return proxyManager.builder()
                    .build("admin:rate-limit:login:".getBytes(), () -> bucketConfig);
        });

        // General admin endpoints: 100 requests/minute per admin
        configurations.put("admin-authenticated", () -> {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(100)
                    .refillIntervally(100, Duration.ofMinutes(1))
                    .build();
            BucketConfiguration bucketConfig = BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
            return proxyManager.builder()
                    .build("admin:rate-limit:authenticated:".getBytes(), () -> bucketConfig);
        });

        log.info("Configured {} admin rate limit configurations", configurations.size());
        return configurations;
    }
}

