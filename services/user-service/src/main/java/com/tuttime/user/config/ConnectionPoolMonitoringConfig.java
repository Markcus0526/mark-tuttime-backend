package com.tuttime.user.config;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;

/**
 * Connection Pool Monitoring Configuration
 * 
 * <p>Provides health checks and monitoring for HikariCP connection pools.
 * Monitors both primary and replica connection pools (if configured).
 * 
 * <p><b>Health Indicators:</b>
 * <ul>
 *   <li>Primary database connection pool health</li>
 *   <li>Replica database connection pool health (if configured)</li>
 * </ul>
 * 
 * <p><b>Metrics Exposed:</b>
 * <ul>
 *   <li>Active connections</li>
 *   <li>Idle connections</li>
 *   <li>Total connections</li>
 *   <li>Threads awaiting connection</li>
 *   <li>Connection pool utilization percentage</li>
 * </ul>
 */
@Slf4j
@Configuration
public class ConnectionPoolMonitoringConfig {

    /**
     * Health indicator for primary database connection pool.
     */
    @Bean
    @ConditionalOnProperty(name = "management.health.db.enabled", havingValue = "true", matchIfMissing = true)
    public HealthIndicator primaryDataSourceHealthIndicator(DataSource dataSource) {
        return new HikariConnectionPoolHealthIndicator(dataSource, "primary");
    }

    /**
     * Custom health indicator for HikariCP connection pools.
     */
    @Slf4j
    private static class HikariConnectionPoolHealthIndicator implements HealthIndicator {
        private final DataSource dataSource;
        private final String poolName;

        public HikariConnectionPoolHealthIndicator(DataSource dataSource, String poolName) {
            this.dataSource = dataSource;
            this.poolName = poolName;
        }

        @Override
        public Health health() {
            if (!(dataSource instanceof HikariDataSource)) {
                return Health.unknown()
                        .withDetail("pool", poolName)
                        .withDetail("reason", "DataSource is not HikariCP")
                        .build();
            }

            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

            if (poolBean == null) {
                return Health.unknown()
                        .withDetail("pool", poolName)
                        .withDetail("reason", "Pool MXBean not available")
                        .build();
            }

            int active = poolBean.getActiveConnections();
            int idle = poolBean.getIdleConnections();
            int total = poolBean.getTotalConnections();
            int threadsAwaiting = poolBean.getThreadsAwaitingConnection();
            int maximumPoolSize = hikariDataSource.getMaximumPoolSize();
            int minimumIdle = hikariDataSource.getMinimumIdle();

            // Calculate utilization percentage
            double utilizationPercent = maximumPoolSize > 0 
                ? (double) total / maximumPoolSize * 100.0 
                : 0.0;

            // Determine health status
            Health.Builder healthBuilder;
            
            // Critical: >90% utilization or threads awaiting >10
            if (utilizationPercent > 90 || threadsAwaiting > 10) {
                healthBuilder = Health.down()
                        .withDetail("pool", poolName)
                        .withDetail("status", "CRITICAL")
                        .withDetail("message", "Connection pool utilization is critical");
            }
            // Warning: >80% utilization or threads awaiting >5
            else if (utilizationPercent > 80 || threadsAwaiting > 5) {
                healthBuilder = Health.status("WARNING")
                        .withDetail("pool", poolName)
                        .withDetail("status", "WARNING")
                        .withDetail("message", "Connection pool utilization is high");
            }
            // Healthy: <80% utilization
            else {
                healthBuilder = Health.up()
                        .withDetail("pool", poolName)
                        .withDetail("status", "HEALTHY");
            }

            // Add detailed metrics
            return healthBuilder
                    .withDetail("activeConnections", active)
                    .withDetail("idleConnections", idle)
                    .withDetail("totalConnections", total)
                    .withDetail("maximumPoolSize", maximumPoolSize)
                    .withDetail("minimumIdle", minimumIdle)
                    .withDetail("threadsAwaitingConnection", threadsAwaiting)
                    .withDetail("utilizationPercent", String.format("%.2f%%", utilizationPercent))
                    .withDetail("connectionTimeout", hikariDataSource.getConnectionTimeout())
                    .withDetail("idleTimeout", hikariDataSource.getIdleTimeout())
                    .withDetail("maxLifetime", hikariDataSource.getMaxLifetime())
                    .build();
        }
    }

    /**
     * Logs connection pool statistics periodically.
     * Useful for monitoring pool usage over time.
     */
    @Bean
    @ConditionalOnProperty(name = "connection.pool.monitoring.enabled", havingValue = "true", matchIfMissing = false)
    public ConnectionPoolLogger connectionPoolLogger(DataSource dataSource) {
        return new ConnectionPoolLogger(dataSource);
    }

    /**
     * Periodically logs connection pool statistics.
     */
    @Slf4j
    private static class ConnectionPoolLogger {
        private final DataSource dataSource;

        public ConnectionPoolLogger(DataSource dataSource) {
            this.dataSource = dataSource;
            if (dataSource instanceof HikariDataSource) {
                startLogging();
            }
        }

        private void startLogging() {
            Thread loggerThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        logConnectionPoolStats();
                        Thread.sleep(TimeUnit.MINUTES.toMillis(5)); // Log every 5 minutes
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 2);
                        log.error("Error logging connection pool stats, stackTrace={}", stackTraceSummary);
                    }
                }
            });
            loggerThread.setDaemon(true);
            loggerThread.setName("connection-pool-logger");
            loggerThread.start();
        }

        private void logConnectionPoolStats() {
            if (!(dataSource instanceof HikariDataSource)) {
                return;
            }

            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariPoolMXBean poolBean = hikariDataSource.getHikariPoolMXBean();

            if (poolBean == null) {
                return;
            }

            int active = poolBean.getActiveConnections();
            int idle = poolBean.getIdleConnections();
            int total = poolBean.getTotalConnections();
            int threadsAwaiting = poolBean.getThreadsAwaitingConnection();
            int maximumPoolSize = hikariDataSource.getMaximumPoolSize();
            double utilizationPercent = maximumPoolSize > 0 
                ? (double) total / maximumPoolSize * 100.0 
                : 0.0;

            log.info("Connection Pool Stats [{}] - Active: {}, Idle: {}, Total: {}/{}, Utilization: {:.2f}%, Threads Awaiting: {}",
                    hikariDataSource.getPoolName(),
                    active, idle, total, maximumPoolSize, utilizationPercent, threadsAwaiting);

            // Log warning if utilization is high
            if (utilizationPercent > 80) {
                log.warn("High connection pool utilization detected: {:.2f}% (Pool: {})", 
                        utilizationPercent, hikariDataSource.getPoolName());
            }

            // Log warning if threads are awaiting connections
            if (threadsAwaiting > 0) {
                log.warn("Threads awaiting connections: {} (Pool: {})", 
                        threadsAwaiting, hikariDataSource.getPoolName());
            }
        }
    }
}

