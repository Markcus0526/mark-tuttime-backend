package com.tuttime.booking.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnProperty(name = "redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Value("${spring.data.redis.ssl:false}")
    private boolean useSsl;

    // Sentinel settings
    @Value("${spring.data.redis.sentinel.master:}")
    private String sentinelMaster;

    @Value("${spring.data.redis.sentinel.nodes:}")
    private String sentinelNodes;

    // Connection pool settings
    @Value("${redisson.connection-min-idle:10}")
    private int minIdle;

    @Value("${redisson.connection-pool-size:64}")
    private int poolSize;

    @Value("${redisson.connect-timeout:30000}")
    private int connectTimeout;

    @Value("${redisson.command-timeout:10000}")
    private int commandTimeout;

    @Value("${redisson.retry-attempts:3}")
    private int retryAttempts;

    @Value("${redisson.retry-interval:1500}")
    private int retryInterval;

    @Bean(destroyMethod = "shutdown")
    @Lazy
    public RedissonClient redissonClient() {
        Config config = new Config();
        String protocol = useSsl ? "rediss://" : "redis://";

        if (sentinelMaster != null && !sentinelMaster.isBlank() && sentinelNodes != null && !sentinelNodes.isBlank()) {
            // Sentinel HA mode
            String[] nodes = sentinelNodes.split(",");
            var sentinelConfig = config.useSentinelServers()
                    .setMasterName(sentinelMaster)
                    .setMasterConnectionMinimumIdleSize(minIdle) // Fixed name
                    .setMasterConnectionPoolSize(poolSize)
                    .setSlaveConnectionMinimumIdleSize(minIdle)  // Fixed name
                    .setSlaveConnectionPoolSize(poolSize)
                    .setConnectTimeout(connectTimeout)
                    .setTimeout(commandTimeout)
                    .setRetryAttempts(retryAttempts)
                    .setRetryInterval(retryInterval);

            if (redisPassword != null && !redisPassword.isBlank()) {
                sentinelConfig.setPassword(redisPassword);
            }

            for (String node : nodes) {
                String formattedNode = node.trim();
                if (!formattedNode.startsWith("redis://") && !formattedNode.startsWith("rediss://")) {
                    formattedNode = protocol + formattedNode;
                }
                sentinelConfig.addSentinelAddress(formattedNode);
            }
        } else {
            // Single Server mode
            String address = protocol + redisHost + ":" + redisPort;
            var singleConfig = config.useSingleServer()
                    .setAddress(address)
                    .setConnectionMinimumIdleSize(minIdle) // Fixed name
                    .setConnectionPoolSize(poolSize)
                    .setConnectTimeout(connectTimeout)
                    .setTimeout(commandTimeout)
                    .setRetryAttempts(retryAttempts)
                    .setRetryInterval(retryInterval);

            if (redisPassword != null && !redisPassword.isBlank()) {
                singleConfig.setPassword(redisPassword);
            }
        }

        return Redisson.create(config);
    }
}
