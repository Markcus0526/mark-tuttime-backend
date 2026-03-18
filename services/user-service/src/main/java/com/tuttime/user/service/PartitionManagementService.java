package com.tuttime.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Partition Management Service: automatically creates monthly partitions for user_activity_logs and audit_logs. Format: {table}_YYYY_MM. Runs daily at 00:00 UTC. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PartitionManagementService {

    private final JdbcTemplate jdbcTemplate;
    
    private static final String SCHEMA = "user_service";
    private static final String TABLE_USER_ACTIVITY_LOGS = "user_activity_logs";
    private static final String TABLE_AUDIT_LOGS = "audit_logs";
    private static final DateTimeFormatter PARTITION_NAME_FORMATTER = DateTimeFormatter.ofPattern("yyyy_MM");

    /** Ensures partition exists for user_activity_logs for the given date. Creates if missing. */
    @Transactional
    public boolean ensureUserActivityLogsPartitionExists(OffsetDateTime date) {
        return ensurePartitionExists(TABLE_USER_ACTIVITY_LOGS, date);
    }

    /** Ensures partition exists for audit_logs for the given date. Creates if missing. */
    @Transactional
    public boolean ensureAuditLogsPartitionExists(OffsetDateTime date) {
        return ensurePartitionExists(TABLE_AUDIT_LOGS, date);
    }

    /** Ensures partitions exist for both tables for the given date. */
    @Transactional
    public void ensureAllPartitionsExist(OffsetDateTime date) {
        ensureUserActivityLogsPartitionExists(date);
        ensureAuditLogsPartitionExists(date);
    }
    
    /** Ensures partitions exist for current and next month on application startup. */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensurePartitionsOnStartup() {
        log.info("Ensuring user-service partitions exist on application startup");
        
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        // Ensure partitions for current month
        try {
            ensureAllPartitionsExist(now);
        } catch (Exception e) {
            log.error("Failed to ensure partitions for current month on startup: {}", e.getMessage());
        }
        
        // Ensure partitions for next month (proactive)
        try {
            ensureAllPartitionsExist(now.plusMonths(1));
        } catch (Exception e) {
            log.error("Failed to ensure partitions for next month on startup: {}", e.getMessage());
        }
        
        log.info("Partition initialization on startup completed");
    }
    
    /**
     * Ensures partitions exist for the current month and next month.
     * This is called by the scheduled task to proactively create partitions.
     */
    @Scheduled(cron = "0 0 0 * * ?") // Daily at midnight UTC
    @Transactional
    public void ensureCurrentAndNextMonthPartitions() {
        log.info("Running scheduled partition creation task for user-service");
        
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        
        // Ensure partitions for current month
        try {
            ensureAllPartitionsExist(now);
        } catch (Exception e) {
            log.error("Failed to ensure partitions for current month: {}", e.getMessage());
        }
        
        // Ensure partitions for next month (proactive)
        try {
            ensureAllPartitionsExist(now.plusMonths(1));
        } catch (Exception e) {
            log.error("Failed to ensure partitions for next month: {}", e.getMessage());
        }
        
        log.info("Scheduled partition creation task completed");
    }
    
    /**
     * Ensures that a partition exists for the given table and date.
     * Creates the partition if it doesn't exist.
     * 
     * @param tableName The table name
     * @param date The date to ensure partition for
     * @return true if partition was created, false if it already existed
     */
    private boolean ensurePartitionExists(String tableName, OffsetDateTime date) {
        String partitionName = getPartitionName(tableName, date);
        OffsetDateTime monthStart = getMonthStart(date);
        OffsetDateTime monthEnd = monthStart.plusMonths(1);
        
        // Check if partition already exists
        if (partitionExists(partitionName)) {
            log.debug("Partition {} already exists for date: {}", partitionName, date);
            return false;
        }
        
        // Create the partition
        try {
            // Format dates for PostgreSQL partition range (YYYY-MM-DD format)
            String monthStartStr = monthStart.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            String monthEndStr = monthEnd.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            
            String createPartitionSql = String.format(
                    "CREATE TABLE IF NOT EXISTS %s.%s PARTITION OF %s.%s " +
                    "FOR VALUES FROM ('%s') TO ('%s')",
                    SCHEMA, partitionName, SCHEMA, tableName,
                    monthStartStr, monthEndStr
            );
            
            log.info("Creating partition {} for date range: {} to {}", 
                    partitionName, monthStartStr, monthEndStr);
            
            jdbcTemplate.execute(createPartitionSql);
            
            log.info("Successfully created partition {} for {} table", partitionName, tableName);
            return true;
        } catch (Exception e) {
            log.error("Failed to create partition {} for date: {}. Error: {}", 
                    partitionName, date, e.getMessage(), e);
            throw new RuntimeException("Failed to create partition: " + partitionName, e);
        }
    }
    
    /**
     * Checks if a partition exists.
     * 
     * @param partitionName The name of the partition to check
     * @return true if partition exists, false otherwise
     */
    private boolean partitionExists(String partitionName) {
        try {
            String checkSql = String.format(
                    "SELECT COUNT(*) FROM pg_class c " +
                    "JOIN pg_namespace n ON n.oid = c.relnamespace " +
                    "WHERE n.nspname = '%s' AND c.relname = '%s'",
                    SCHEMA, partitionName
            );
            
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class);
            return count != null && count > 0;
        } catch (Exception e) {
            log.warn("Error checking partition existence for {}: {}", partitionName, e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the partition name for a given table and date.
     * Format: {table_name}_YYYY_MM
     * 
     * @param tableName The table name
     * @param date The date
     * @return The partition name
     */
    private String getPartitionName(String tableName, OffsetDateTime date) {
        return tableName + "_" + date.format(PARTITION_NAME_FORMATTER);
    }
    
    /**
     * Gets the start of the month for a given date.
     * 
     * @param date The date
     * @return The start of the month (first day, 00:00:00)
     */
    private OffsetDateTime getMonthStart(OffsetDateTime date) {
        return date.withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0);
    }
}

