package com.tuttime.booking.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl:false}")
    private boolean useSsl;

    @Bean(destroyMethod = "shutdown") // Ensures graceful release of connections on app stop
    @Lazy
    public RedissonClient redissonClient() {
        Config config = new Config();

        // Protocol: "rediss://" for SSL/TLS, "redis://" for standard
        String protocol = useSsl ? "rediss://" : "redis://";
        String address = String.format("%s%s:%d", protocol, redisHost, redisPort);

        config.useSingleServer()
                .setAddress(address)
                .setPassword(redisPassword.isEmpty() ? null : redisPassword)

                // --- Performance & Pooling ---
                .setConnectionMinimumIdleSize(10)     // Keep 10 connections warm
                .setConnectionPoolSize(64)            // Scale up to 64 under load
                .setIdleConnectionTimeout(10000)      // Close idle connections after 10s

                // --- Resiliency & Timeouts ---
                .setConnectTimeout(5000)             // 10s to establish connection
                .setTimeout(3000)                     // 3s wait for command response
                .setRetryAttempts(10)                  // Retry failed commands 3 times
                .setRetryInterval(2000)               // Wait 1.5s between retries

                // --- Reliability ---
                .setPingConnectionInterval(30000);    // Keep-alive ping every 30s

        return Redisson.create(config);
    }
}
