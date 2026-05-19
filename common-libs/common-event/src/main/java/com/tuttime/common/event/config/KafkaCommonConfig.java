package com.tuttime.common.event.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Shared Kafka Configuration Properties
 *
 * <p>Centralized Kafka configuration that can be shared across all services.
 * This ensures consistent configuration and makes it easier to manage Kafka settings.
 */
@Configuration
@ConfigurationProperties(prefix = "spring.kafka")
@Getter
@Setter
public class KafkaCommonConfig {

    // Getters and Setters
    /**
     * Kafka bootstrap servers
     * Default: localhost:29092 (local) or tuttime-kafka:9092 (Docker)
     */
    private String bootstrapServers = "localhost:29092";

    /**
     * Consumer group ID prefix
     * Services should append their service name: {groupPrefix}-{serviceName}
     */
    private String consumerGroupPrefix = "tuttime";

    /**
     * Default consumer concurrency
     */
    private int defaultConcurrency = 2;

    /**
     * Default number of partitions for topics
     */
    private int defaultPartitions = 3;

    /**
     * Default replication factor for topics
     */
    private short defaultReplicationFactor = 1;

    /**
     * Enable auto-commit by default
     */
    private boolean defaultAutoCommit = true;

    /**
     * Default max poll records
     */
    private int defaultMaxPollRecords = 100;

    /**
     * Default max poll interval (ms)
     */
    private long defaultMaxPollIntervalMs = 300000L; // 5 minutes

    /**
     * Default session timeout (ms)
     */
    private long defaultSessionTimeoutMs = 10000L; // 10 seconds

    /**
     * Default heartbeat interval (ms)
     */
    private long defaultHeartbeatIntervalMs = 3000L; // 3 seconds

}

