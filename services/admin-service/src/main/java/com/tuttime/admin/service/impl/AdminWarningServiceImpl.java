package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.WarningCategory;
import com.tuttime.admin.dto.WarningResponse;
import com.tuttime.admin.dto.WarningSeverity;
import com.tuttime.admin.dto.WarningStatisticsResponse;
import com.tuttime.admin.service.AdminAntiFraudService;
import com.tuttime.admin.service.AdminWarningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Admin Warning Service Implementation
 * 
 * <p>Aggregates warnings from various sources:
 * <ul>
 *   <li>Fraud flags from AdminAntiFraudService</li>
 *   <li>Brute-force attempts from Redis</li>
 *   <li>System stability warnings (from logs or metrics)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminWarningServiceImpl implements AdminWarningService {

    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    private final AdminAntiFraudService antiFraudService;

    // Redis key prefixes for brute-force data
    private static final String BRUTEFORCE_ATTEMPT_PREFIX = "bruteforce:login:";
    private static final String BRUTEFORCE_BLOCKED_IP_PREFIX = "bruteforce:blocked:ip:";
    private static final String BRUTEFORCE_BLOCKED_PHONE_PREFIX = "bruteforce:blocked:phone:";
    
    // In-memory storage for system stability warnings (could be replaced with database)
    private final Map<String, WarningResponse> systemStabilityWarnings = new HashMap<>();

    @Override
    public Page<WarningResponse> getConsolidatedWarnings(
            Pageable pageable,
            WarningCategory category,
            WarningSeverity severity,
            Boolean resolved,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        
        log.debug("Getting consolidated warnings: category={}, severity={}, resolved={}", 
                category, severity, resolved);
        
        // TODO: Replace with database query after creating warnings table
        // 1. Create WarningRepository with query methods:
        //    - findByCategoryAndSeverityAndResolvedAndCreatedAtBetween(...)
        // 2. Query warnings table with filters:
        //    SELECT * FROM warnings 
        //    WHERE (category = ? OR ? IS NULL)
        //      AND (severity = ? OR ? IS NULL)
        //      AND (resolved = ? OR ? IS NULL)
        //      AND (created_at >= ? OR ? IS NULL)
        //      AND (created_at <= ? OR ? IS NULL)
        //    ORDER BY severity DESC, created_at DESC
        // 3. Use Spring Data JPA pagination
        // 4. Remove aggregation logic below once database is integrated
        
        List<WarningResponse> allWarnings = new ArrayList<>();
        
        // Get fraud flags
        try {
            Page<com.tuttime.admin.dto.FraudFlagResponse> fraudFlags = 
                    antiFraudService.listFraudFlags(pageable, null, null, null, null);
            fraudFlags.getContent().forEach(flag -> {
                WarningResponse warning = convertFraudFlagToWarning(flag);
                if (matchesFilters(warning, category, severity, resolved, startDate, endDate)) {
                    allWarnings.add(warning);
                }
            });
        } catch (Exception e) {
            log.error("Error fetching fraud flags", e);
        }
        
        // Get brute-force warnings
        try {
            List<WarningResponse> bruteForceWarnings = getBruteForceWarningsFromRedis();
            bruteForceWarnings.stream()
                    .filter(w -> matchesFilters(w, category, severity, resolved, startDate, endDate))
                    .forEach(allWarnings::add);
        } catch (Exception e) {
            log.error("Error fetching brute-force warnings", e);
        }
        
        // Get system stability warnings
        try {
            systemStabilityWarnings.values().stream()
                    .filter(w -> matchesFilters(w, category, severity, resolved, startDate, endDate))
                    .forEach(allWarnings::add);
        } catch (Exception e) {
            log.error("Error fetching system stability warnings", e);
        }
        
        // Sort by severity (CRITICAL > HIGH > MEDIUM > LOW) and then by date (newest first)
        allWarnings.sort((a, b) -> {
            int severityCompare = getSeverityOrder(b.getSeverity()) - getSeverityOrder(a.getSeverity());
            if (severityCompare != 0) {
                return severityCompare;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allWarnings.size());
        List<WarningResponse> pageContent = start < allWarnings.size() 
                ? allWarnings.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, allWarnings.size());
    }

    @Override
    public Page<WarningResponse> getBruteForceWarnings(
            Pageable pageable,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        
        log.debug("Getting brute-force warnings");
        
        // TODO: Integrate with brute-force service for brute-force warnings/history
        // 1. Create WarningRepository to persist warnings in database
        // 2. Replace Redis-based brute-force warnings with database queries
        // 3. Integrate with user-service brute-force tracking:
        //    - Call user-service gRPC endpoint to get brute-force attempts
        //    - Or query brute_force_attempts table in user-service database
        // 4. Persist brute-force warnings to warnings table:
        //    - Save when brute-force attempt threshold is reached
        //    - Update when IP/phone is blocked
        //    - Mark as resolved when block expires or is manually resolved
        // 5. Query warnings table filtered by:
        //    - category = 'BRUTE_FORCE'
        //    - source = 'BRUTE_FORCE'
        //    - date range, IP address, phone number
        
        List<WarningResponse> warnings = getBruteForceWarningsFromRedis();
        
        // Filter by date range
        if (startDate != null || endDate != null) {
            warnings = warnings.stream()
                    .filter(w -> {
                        OffsetDateTime createdAt = w.getCreatedAt();
                        if (startDate != null && createdAt != null && createdAt.isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && createdAt != null && createdAt.isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        
        // Sort by date (newest first)
        warnings.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), warnings.size());
        List<WarningResponse> pageContent = start < warnings.size() 
                ? warnings.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, warnings.size());
    }

    @Override
    public Page<WarningResponse> getSystemStabilityWarnings(
            Pageable pageable,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        
        log.debug("Getting system stability warnings");
        
        // TODO: Integrate with system monitoring for stability warnings/history
        // 1. Create WarningRepository to persist warnings in database
        // 2. Replace in-memory systemStabilityWarnings with database queries
        // 3. Integrate with system monitoring service (if exists) or implement monitoring:
        //    - Monitor error rates per service (ERROR_RATE event type)
        //    - Monitor response times (RESPONSE_TIME event type)
        //    - Monitor memory usage (MEMORY_USAGE event type)
        //    - Monitor CPU usage (CPU_USAGE event type)
        //    - Monitor database connection pool (DATABASE_ERROR event type)
        // 4. Set up scheduled jobs to check metrics:
        //    - Check error rate threshold (e.g., > 5% errors in last 5 minutes)
        //    - Check response time threshold (e.g., p95 > 2 seconds)
        //    - Check memory usage threshold (e.g., > 80% memory used)
        // 5. Create warnings when thresholds are exceeded:
        //    - Save to warnings table with category = 'SYSTEM_STABILITY'
        //    - Include service name, event type, and metrics in metadata
        // 6. Query warnings table filtered by:
        //    - category = 'SYSTEM_STABILITY'
        //    - source = 'SYSTEM_STABILITY'
        //    - eventType (from metadata)
        //    - serviceName (from metadata)
        //    - date range
        
        List<WarningResponse> warnings = new ArrayList<>(systemStabilityWarnings.values());
        
        // Filter by date range
        if (startDate != null || endDate != null) {
            warnings = warnings.stream()
                    .filter(w -> {
                        OffsetDateTime createdAt = w.getCreatedAt();
                        if (startDate != null && createdAt != null && createdAt.isBefore(startDate)) {
                            return false;
                        }
                        if (endDate != null && createdAt != null && createdAt.isAfter(endDate)) {
                            return false;
                        }
                        return true;
                    })
                    .collect(Collectors.toList());
        }
        
        // Sort by severity and date
        warnings.sort((a, b) -> {
            int severityCompare = getSeverityOrder(b.getSeverity()) - getSeverityOrder(a.getSeverity());
            if (severityCompare != 0) {
                return severityCompare;
            }
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        
        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), warnings.size());
        List<WarningResponse> pageContent = start < warnings.size() 
                ? warnings.subList(start, end) 
                : new ArrayList<>();
        
        return new PageImpl<>(pageContent, pageable, warnings.size());
    }

    @Override
    public void resolveWarning(String warningId) {
        log.info("Resolving warning: {}", warningId);
        
        // TODO: Persist warning resolution to database
        // 1. Create WarningRepository to update warnings table
        // 2. Update warning in database:
        //    UPDATE warnings SET resolved = true, resolved_at = NOW(), resolved_by = ? WHERE warning_id = ?
        // 3. For in-memory system stability warnings, also update database if persisted
        // 4. For Redis-based brute-force warnings, update database if persisted
        // 5. Remove from in-memory cache if using database
        
        // Check if it's a system stability warning
        if (systemStabilityWarnings.containsKey(warningId)) {
            WarningResponse warning = systemStabilityWarnings.get(warningId);
            warning.setResolved(true);
            warning.setResolvedAt(OffsetDateTime.now(ZoneOffset.UTC));
            systemStabilityWarnings.put(warningId, warning);
        } else {
            log.warn("Warning not found: {}", warningId);
        }
    }

    @Override
    public WarningStatisticsResponse getWarningStatistics() {
        // TODO: Implement actual statistics from database or aggregated sources
        // 1. Create WarningRepository to query warnings table
        // 2. Query database for:
        //    - Total warnings count: SELECT COUNT(*) FROM warnings
        //    - Unresolved warnings: SELECT COUNT(*) FROM warnings WHERE resolved = false
        //    - By category: SELECT category, COUNT(*) FROM warnings GROUP BY category
        //    - By severity: SELECT severity, COUNT(*) FROM warnings GROUP BY severity
        // 3. Convert enum values to strings for response
        // 4. Cache statistics for performance (refresh every 5 minutes)
        // For now, return placeholder data
        return WarningStatisticsResponse.builder()
                .totalWarnings(0L)
                .unresolvedWarnings(0L)
                .byCategory(new HashMap<>())
                .bySeverity(new HashMap<>())
                .build();
    }

    /**
     * Converts a FraudFlagResponse to a WarningResponse
     */
    private WarningResponse convertFraudFlagToWarning(com.tuttime.admin.dto.FraudFlagResponse flag) {
        WarningCategory category = flag.getCategory() != null 
                ? flag.getCategory() 
                : determineCategoryFromFlagType(flag.getFlagType());
        
        WarningSeverity severity = flag.getSeverity() != null 
                ? flag.getSeverity() 
                : WarningSeverity.MEDIUM;
        
        String message = flag.getDescription() != null ? flag.getDescription() : "Fraud flag detected: " + flag.getFlagType();
        return WarningResponse.builder()
                .warningId("FRAUD_" + flag.getFlagId())
                .category(category)
                .severity(severity)
                .message(message)
                .title("Fraud Flag: " + flag.getFlagType())
                .description(flag.getDescription())
                .createdAt(flag.getCreatedAt())
                .resolvedAt(null)
                .resolved(flag.getResolved() != null ? flag.getResolved() : false)
                .userId(flag.getUserId())
                .deviceId(flag.getDeviceId())
                .ipAddress(flag.getIpAddress())
                .source("ANTI_FRAUD")
                .build();
    }

    /**
     * Determines WarningCategory from flag type string
     */
    private WarningCategory determineCategoryFromFlagType(String flagType) {
        if (flagType == null) {
            return WarningCategory.OTHER;
        }
        
        String upperType = flagType.toUpperCase();
        if (upperType.contains("MULTI_DEVICE")) {
            return WarningCategory.MULTI_DEVICE;
        } else if (upperType.contains("MULTI_ACCOUNT")) {
            return WarningCategory.MULTI_ACCOUNT;
        } else if (upperType.contains("SUSPICIOUS")) {
            return WarningCategory.SUSPICIOUS;
        } else if (upperType.contains("MALICIOUS")) {
            return WarningCategory.MALICIOUS;
        } else {
            return WarningCategory.FRAUD;
        }
    }

    /**
     * Gets brute-force warnings from Redis
     */
    /**
     * Gets brute-force warnings from Redis
     * 
     * TODO: Replace with database queries after integrating with brute-force service
     * This method should be refactored to query warnings table instead of Redis
     */
    private List<WarningResponse> getBruteForceWarningsFromRedis() {
        List<WarningResponse> warnings = new ArrayList<>();
        
        if (redisTemplate == null) {
            log.debug("Redis not available - skipping brute-force warnings");
            return warnings;
        }
        
        try {
            // Get blocked IPs
            Set<String> blockedIpKeys = redisTemplate.keys(BRUTEFORCE_BLOCKED_IP_PREFIX + "*");
            if (blockedIpKeys != null) {
                for (String key : blockedIpKeys) {
                    String ipAddress = key.substring(BRUTEFORCE_BLOCKED_IP_PREFIX.length());
                    String blockUntilStr = redisTemplate.opsForValue().get(key);
                    
                    if (blockUntilStr != null) {
                        try {
                            long blockUntil = Long.parseLong(blockUntilStr);
                            long now = System.currentTimeMillis() / 1000;
                            
                            if (blockUntil > now) {
                                // Still blocked
                                WarningSeverity severity = determineBruteForceSeverity(ipAddress, null);
                                
                                String message = String.format("IP address %s blocked due to brute-force attempts", ipAddress);
                                warnings.add(WarningResponse.builder()
                                        .warningId("BF_IP_" + ipAddress)
                                        .category(WarningCategory.BRUTE_FORCE)
                                        .severity(severity)
                                        .message(message)
                                        .title("Brute-Force Attack: IP Blocked")
                                        .description(message)
                                        .ipAddress(ipAddress)
                                        .createdAt(OffsetDateTime.ofInstant(
                                                Instant.ofEpochSecond(blockUntil - (15 * 60), 0), ZoneOffset.UTC))
                                        .resolved(false)
                                        .source("BRUTE_FORCE")
                                        .metadata(Map.of("blockUntil", blockUntil))
                                        .build());
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid block timestamp for IP: {}", ipAddress, e);
                        }
                    }
                }
            }
            
            // Get blocked phone numbers
            Set<String> blockedPhoneKeys = redisTemplate.keys(BRUTEFORCE_BLOCKED_PHONE_PREFIX + "*");
            if (blockedPhoneKeys != null) {
                for (String key : blockedPhoneKeys) {
                    String phoneNumber = key.substring(BRUTEFORCE_BLOCKED_PHONE_PREFIX.length());
                    String blockUntilStr = redisTemplate.opsForValue().get(key);
                    
                    if (blockUntilStr != null) {
                        try {
                            long blockUntil = Long.parseLong(blockUntilStr);
                            long now = System.currentTimeMillis() / 1000;
                            
                            if (blockUntil > now) {
                                // Still blocked
                                WarningSeverity severity = determineBruteForceSeverity(null, phoneNumber);
                                
                                String message = String.format("Phone number %s blocked due to brute-force attempts", phoneNumber);
                                warnings.add(WarningResponse.builder()
                                        .warningId("BF_PHONE_" + phoneNumber)
                                        .category(WarningCategory.BRUTE_FORCE)
                                        .severity(severity)
                                        .message(message)
                                        .title("Brute-Force Attack: Phone Blocked")
                                        .description(message)
                                        .phoneNumber(phoneNumber)
                                        .createdAt(OffsetDateTime.ofInstant(
                                                Instant.ofEpochSecond(blockUntil - (60 * 60), 0), ZoneOffset.UTC))
                                        .resolved(false)
                                        .source("BRUTE_FORCE")
                                        .metadata(Map.of("blockUntil", blockUntil))
                                        .build());
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid block timestamp for phone: {}", phoneNumber, e);
                        }
                    }
                }
            }
            
            // Get active attempt counters (warnings but not yet blocked)
            Set<String> attemptKeys = redisTemplate.keys(BRUTEFORCE_ATTEMPT_PREFIX + "*");
            if (attemptKeys != null) {
                for (String key : attemptKeys) {
                    String value = redisTemplate.opsForValue().get(key);
                    if (value != null) {
                        try {
                            long attempts = Long.parseLong(value);
                            if (attempts >= 3) { // Warning threshold
                                String[] parts = key.substring(BRUTEFORCE_ATTEMPT_PREFIX.length()).split(":");
                                if (parts.length >= 2) {
                                    String ipAddress = parts[0];
                                    String phoneNumber = parts[1];
                                    
                                    WarningSeverity severity = determineBruteForceSeverity(ipAddress, phoneNumber);
                                    
                                    String message = String.format("Multiple failed login attempts detected: %d attempts from IP %s", attempts, ipAddress);
                                    warnings.add(WarningResponse.builder()
                                            .warningId("BF_ATTEMPT_" + key.hashCode())
                                            .category(WarningCategory.BRUTE_FORCE)
                                            .severity(severity)
                                            .message(message)
                                            .title("Brute-Force Warning: Multiple Failed Attempts")
                                            .description(message)
                                            .ipAddress(ipAddress)
                                            .phoneNumber(phoneNumber)
                                            .createdAt(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)) // Approximate
                                            .resolved(false)
                                            .source("BRUTE_FORCE")
                                            .metadata(Map.of("attempts", attempts))
                                            .build());
                                }
                            }
                        } catch (NumberFormatException e) {
                            log.warn("Invalid attempt count for key: {}", key, e);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            log.error("Error fetching brute-force warnings from Redis", e);
        }
        
        return warnings;
    }

    /**
     * Determines severity based on brute-force attempt count
     */
    private WarningSeverity determineBruteForceSeverity(String ipAddress, String phoneNumber) {
        if (redisTemplate == null) {
            return WarningSeverity.MEDIUM;
        }
        
        try {
            // Check attempt count
            if (ipAddress != null && phoneNumber != null) {
                String attemptKey = BRUTEFORCE_ATTEMPT_PREFIX + ipAddress + ":" + phoneNumber;
                String value = redisTemplate.opsForValue().get(attemptKey);
                if (value != null) {
                    long attempts = Long.parseLong(value);
                    if (attempts >= 15) {
                        return WarningSeverity.CRITICAL;
                    } else if (attempts >= 10) {
                        return WarningSeverity.HIGH;
                    } else if (attempts >= 5) {
                        return WarningSeverity.MEDIUM;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error determining brute-force severity", e);
        }
        
        return WarningSeverity.MEDIUM;
    }

    /**
     * Checks if a warning matches the given filters
     */
    private boolean matchesFilters(
            WarningResponse warning,
            WarningCategory category,
            WarningSeverity severity,
            Boolean resolved,
            OffsetDateTime startDate,
            OffsetDateTime endDate) {
        
        if (category != null && warning.getCategory() != category) {
            return false;
        }
        
        if (severity != null && warning.getSeverity() != severity) {
            return false;
        }
        
        if (resolved != null && !resolved.equals(warning.getResolved())) {
            return false;
        }
        
        if (startDate != null && warning.getCreatedAt().isBefore(startDate)) {
            return false;
        }
        
        if (endDate != null && warning.getCreatedAt().isAfter(endDate)) {
            return false;
        }
        
        return true;
    }

    /**
     * Gets severity order for sorting (higher number = higher priority)
     */
    private int getSeverityOrder(WarningSeverity severity) {
        if (severity == null) {
            return 0;
        }
        switch (severity) {
            case CRITICAL: return 4;
            case HIGH: return 3;
            case MEDIUM: return 2;
            case LOW: return 1;
            default: return 0;
        }
    }

}

