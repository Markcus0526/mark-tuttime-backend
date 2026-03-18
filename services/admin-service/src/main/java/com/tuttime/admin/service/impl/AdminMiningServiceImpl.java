package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.*;
import com.tuttime.admin.grpc.client.MiningServiceGrpcClient;
import com.tuttime.admin.grpc.client.UserServiceGrpcClient;
// TODO: Uncomment when token-service is deployed
// import com.tuttime.admin.grpc.client.TokenServiceGrpcClient;
import com.tuttime.admin.grpc.client.RewardsServiceGrpcClient;
import com.tuttime.admin.rest.client.MiningServiceRestClient;
import com.tuttime.admin.service.AdminMiningService;
import com.tuttime.common.proto.GetMiningSpeedResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Admin Mining Service Implementation
 * 
 * <p>Integrates with mining-service via gRPC to provide AP Credits management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMiningServiceImpl implements AdminMiningService {

    private final MiningServiceGrpcClient miningServiceGrpcClient;
    private final MiningServiceRestClient miningServiceRestClient;
    private final UserServiceGrpcClient userServiceGrpcClient;
    // TODO: Uncomment when token-service is deployed
    // private final TokenServiceGrpcClient tokenServiceGrpcClient;
    private final RewardsServiceGrpcClient rewardsServiceGrpcClient;
    
    // Executor for parallel service calls
    private final Executor serviceCallExecutor = Executors.newFixedThreadPool(10);

    @Override
    public Page<com.tuttime.admin.dto.ApBalanceResponse> listApBalances(String search, Pageable pageable) {
        try {
            log.debug("Listing AP balances: search={}, page={}, size={}", search, pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Map<String, Object>> balancesPage = miningServiceRestClient.listApBalances(pageable);
            
            log.debug("Received {} balances from mining service (total: {})", 
                    balancesPage.getContent().size(), balancesPage.getTotalElements());
            
            // Map to admin DTOs
            List<com.tuttime.admin.dto.ApBalanceResponse> balanceResponses = balancesPage.getContent().stream()
                    .map(balanceData -> {
                        log.debug("Processing balance data: {}", balanceData);
                        return mapToApBalanceResponse(balanceData);
                    })
                    .collect(Collectors.toList());
            
            log.debug("Mapped {} balance responses. Sample first balance: userId={}, currentBalance={}", 
                    balanceResponses.size(),
                    balanceResponses.isEmpty() ? null : balanceResponses.get(0).getUserId(),
                    balanceResponses.isEmpty() ? null : balanceResponses.get(0).getCurrentBalance());
            
            // Apply search filter if provided
            if (search != null && !search.trim().isEmpty()) {
                String searchLower = search.toLowerCase();
                balanceResponses = balanceResponses.stream()
                        .filter(balance -> {
                            String username = balance.getUsername() != null ? balance.getUsername().toLowerCase() : "";
                            String email = balance.getEmail() != null ? balance.getEmail().toLowerCase() : "";
                            String userIdStr = balance.getUserId() != null ? balance.getUserId().toString() : "";
                            return username.contains(searchLower) || email.contains(searchLower) || userIdStr.contains(searchLower);
                        })
                        .collect(Collectors.toList());
            }
            
            log.debug("Returning {} filtered balances", balanceResponses.size());
            return new PageImpl<>(
                    balanceResponses,
                    PageRequest.of(balancesPage.getNumber(), balancesPage.getSize()),
                    balancesPage.getTotalElements()
            );
        } catch (Exception e) {
            log.error("Error listing AP balances: {}", e.getMessage(), e);
            // Return static empty page when mining-service is unavailable
            return new PageImpl<>(
                    List.of(),
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                    0L
            );
        }
    }

    private com.tuttime.admin.dto.ApBalanceResponse mapToApBalanceResponse(Map<String, Object> data) {
        Long userId = getLongValue(data, "userId");
        BigDecimal currentBalance = getBigDecimalValue(data, "currentBalance");
        BigDecimal lifetimeAccumulated = getBigDecimalValue(data, "lifetimeAccumulated");
        OffsetDateTime lastUpdated = parseDateTime(data.get("lastUpdated"));
        
        // Debug logging to trace AP balance data
        log.debug("Mapping AP balance for userId={}, raw data keys: {}, currentBalance={}, lifetimeAccumulated={}", 
                userId, data.keySet(), currentBalance, lifetimeAccumulated);
        
        // Fetch additional user data in parallel for better performance
        String email = "";
        String username = "";
        String statusLevel = "BRONZE";
        Integer referralCount = 0;
        OffsetDateTime createdAt = null;
        
        if (userId != null) {
            try {
                // Parallel fetching of user data
                CompletableFuture<com.tuttime.common.proto.UserResponse> userFuture = 
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return userServiceGrpcClient.getUserById(userId);
                        } catch (Exception e) {
                            log.debug("Failed to fetch user for userId {}: {}", userId, e.getMessage());
                            return null;
                        }
                    }, serviceCallExecutor);
                
                // TODO: Uncomment when token-service is deployed
                CompletableFuture<com.tuttime.common.proto.GetUserStatusResponse> statusFuture = 
                    CompletableFuture.supplyAsync(() -> {
                        // try {
                        //     return tokenServiceGrpcClient.getUserStatus(userId);
                        // } catch (Exception e) {
                        //     log.debug("Failed to fetch user status for userId {}: {}", userId, e.getMessage());
                        //     return null;
                        // }
                        // Token service not deployed
                        log.debug("Token service not deployed - returning null for user status");
                        return null;
                    }, serviceCallExecutor);
                
                CompletableFuture<com.tuttime.common.proto.ActiveFriendsCountResponse> referralFuture = 
                    CompletableFuture.supplyAsync(() -> {
                        try {
                            return rewardsServiceGrpcClient.getActiveFriendsCount(userId);
                        } catch (Exception e) {
                            log.debug("Failed to fetch referral count for userId {}: {}", userId, e.getMessage());
                            return null;
                        }
                    }, serviceCallExecutor);
                
                // Wait for all calls with timeout (2 seconds max per call)
                try {
                    com.tuttime.common.proto.UserResponse user = userFuture.get(2, TimeUnit.SECONDS);
                    if (user != null) {
                        email = user.getEmail() != null ? user.getEmail() : "";
                        String firstName = user.getFirstName() != null ? user.getFirstName() : "";
                        String lastName = user.getLastName() != null ? user.getLastName() : "";
                        username = (firstName + " " + lastName).trim();
                        if (user.getRegistrationDate() > 0) {
                            createdAt = OffsetDateTime.ofInstant(
                                Instant.ofEpochMilli(user.getRegistrationDate()), 
                                ZoneOffset.UTC
                            );
                        }
                    }
                } catch (Exception e) {
                    log.debug("Timeout or error getting user for userId {}: {}", userId, e.getMessage());
                }
                
                try {
                    com.tuttime.common.proto.GetUserStatusResponse status = statusFuture.get(2, TimeUnit.SECONDS);
                    if (status != null && status.getStatusLevel() != null) {
                        statusLevel = status.getStatusLevel();
                    }
                } catch (Exception e) {
                    log.debug("Timeout or error getting status for userId {}: {}", userId, e.getMessage());
                }
                
                try {
                    com.tuttime.common.proto.ActiveFriendsCountResponse referral = referralFuture.get(2, TimeUnit.SECONDS);
                    if (referral != null) {
                        referralCount = referral.getCount();
                    }
                } catch (Exception e) {
                    log.debug("Timeout or error getting referral count for userId {}: {}", userId, e.getMessage());
                }
            } catch (Exception e) {
                log.warn("Error fetching additional user data for userId {}: {}", userId, e.getMessage());
            }
        }
        
        // Use BigDecimal directly to preserve decimal precision (e.g., 0.473)
        // No conversion needed - BigDecimal serializes to number in JSON
        com.tuttime.admin.dto.ApBalanceResponse response = com.tuttime.admin.dto.ApBalanceResponse.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .currentBalance(currentBalance != null ? currentBalance : BigDecimal.ZERO)
                .totalEarned(lifetimeAccumulated != null ? lifetimeAccumulated : BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO) // TODO: Calculate from transaction history
                .statusLevel(statusLevel)
                .referralCount(referralCount)
                .createdAt(createdAt)
                .lastUpdated(lastUpdated != null ? lastUpdated : OffsetDateTime.now(ZoneOffset.UTC))
                .build();
        
        log.debug("Final ApBalanceResponse for userId={}: currentBalance={}, totalEarned={}", 
                userId, response.getCurrentBalance(), response.getTotalEarned());
        
        return response;
    }

    @Override
    public com.tuttime.admin.dto.ApBalanceResponse getUserApBalance(Long userId) {
        try {
            log.debug("Getting AP balance via REST: userId={}", userId);
            
            // Use REST client to get balance from mining service
            Map<String, Object> balanceData = miningServiceRestClient.getUserApBalance(userId);
            
            if (balanceData == null || balanceData.isEmpty()) {
                log.warn("AP balance data is empty for userId={}", userId);
                return buildDefaultApBalanceResponse(userId);
            }
            
            log.debug("Mapping AP balance data for userId={}", userId);
            return mapToApBalanceResponse(balanceData);
        } catch (Exception e) {
            log.error("Error getting AP balance for userId={}: {}", userId, e.getMessage(), e);
            return buildDefaultApBalanceResponse(userId);
        }
    }
    
    private com.tuttime.admin.dto.ApBalanceResponse buildDefaultApBalanceResponse(Long userId) {
        return com.tuttime.admin.dto.ApBalanceResponse.builder()
                .userId(userId)
                .username("")
                .email("")
                .currentBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalSpent(BigDecimal.ZERO)
                .statusLevel("BRONZE")
                .referralCount(0)
                .createdAt(null)
                .lastUpdated(OffsetDateTime.now(ZoneOffset.UTC))
                .build();
    }
    
    private Long parseBigDecimalToLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return new BigDecimal(value).longValue();
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal to Long: {}", value, e);
            return 0L;
        }
    }

    @Override
    public Page<MiningSessionResponse> getMiningSessions(Long userId, Pageable pageable) {
        try {
            log.debug("Getting mining sessions: userId={}, page={}, size={}", userId, pageable.getPageNumber(), pageable.getPageSize());
            
            Page<Map<String, Object>> sessionsPage = miningServiceRestClient.getUserMiningSessions(userId, pageable);
            
            log.debug("Received {} mining sessions from mining service for userId={} (total: {})", 
                    sessionsPage.getContent().size(), userId, sessionsPage.getTotalElements());
            
            // Map to admin DTOs
            List<MiningSessionResponse> sessionResponses = sessionsPage.getContent().stream()
                    .map(data -> {
                        log.debug("Mapping mining session data: {}", data);
                        return mapToMiningSessionResponse(data);
                    })
                    .collect(Collectors.toList());
            
            log.debug("Returning {} mapped mining sessions for userId={}", sessionResponses.size(), userId);
            return new PageImpl<>(
                    sessionResponses,
                    PageRequest.of(sessionsPage.getNumber(), sessionsPage.getSize()),
                    sessionsPage.getTotalElements()
            );
        } catch (Exception e) {
            log.error("Error getting mining sessions for userId={}: {}", userId, e.getMessage(), e);
            // Return static empty page when mining-service is unavailable
            return new PageImpl<>(
                    List.of(),
                    PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                    0L
            );
        }
    }

    private MiningSessionResponse mapToMiningSessionResponse(Map<String, Object> data) {
        return MiningSessionResponse.builder()
                .sessionId(getLongValue(data, "userId")) // Using userId as sessionId since it's the primary key
                .userId(getLongValue(data, "userId"))
                .startTime(parseDateTime(data.get("startedAt")))
                .endTime(parseDateTime(data.get("endedAt")))
                .apEarned(getBigDecimalValue(data, "totalApEarned") != null 
                        ? getBigDecimalValue(data, "totalApEarned").longValue() : 0L)
                .miningSpeed(getBigDecimalValue(data, "miningSpeed") != null 
                        ? getBigDecimalValue(data, "miningSpeed").doubleValue() : 0.0)
                .isActive(getBooleanValue(data, "isActive"))
                // Detailed speed breakdown fields
                .baseSpeed(getBigDecimalValue(data, "baseSpeed"))
                .friendBonusSpeed(getBigDecimalValue(data, "friendBonusSpeed"))
                .bookingBonusSpeed(getBigDecimalValue(data, "bookingBonusSpeed"))
                .videoBoostSpeed(getBigDecimalValue(data, "videoBoostSpeed"))
                .statusBonusSpeed(getBigDecimalValue(data, "statusBonusSpeed"))
                .taskBonusSpeed(getBigDecimalValue(data, "taskBonusSpeed"))
                .streakBonusSpeed(getBigDecimalValue(data, "streakBonusSpeed"))
                .stakingBonusSpeed(getBigDecimalValue(data, "stakingBonusSpeed"))
                .activeMiningBonusSpeed(getBigDecimalValue(data, "activeMiningBonusSpeed"))
                .isCapped(getBooleanValue(data, "isCapped"))
                .build();
    }

    private Long getLongValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            log.warn("Failed to parse long value for key {}: {}", key, value);
            return null;
        }
    }

    private BigDecimal getBigDecimalValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            log.debug("Value for key '{}' is null, returning BigDecimal.ZERO", key);
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            log.debug("Value for key '{}' is BigDecimal: {}", key, value);
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            // Use string conversion to preserve precision for BigDecimal
            BigDecimal result = new BigDecimal(value.toString());
            log.debug("Value for key '{}' is Number ({}), converted to BigDecimal: {}", key, value.getClass().getSimpleName(), result);
            return result;
        }
        try {
            BigDecimal result = new BigDecimal(value.toString());
            log.debug("Value for key '{}' is String/other ({}), converted to BigDecimal: {}", key, value.getClass().getSimpleName(), result);
            return result;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse BigDecimal value for key {}: {} (type: {})", key, value, value != null ? value.getClass().getSimpleName() : "null");
            return BigDecimal.ZERO;
        }
    }

    private Boolean getBooleanValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return false;
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString());
    }

    /**
     * Parses a datetime string to OffsetDateTime.
     * 
     * <p><b>Timezone Note:</b> Parsed dates are assumed to be in UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param value The datetime value to parse (String or null)
     * @return Parsed OffsetDateTime in UTC context, or null if parsing fails
     */
    private OffsetDateTime parseDateTime(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof String) {
                // Parses ISO-8601 format with timezone (assumed UTC from API responses)
                return OffsetDateTime.parse((String) value);
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", value, e);
            return null;
        }
    }

    @Override
    public MiningSpeedResponse getMiningSpeed(Long userId) {
        try {
            GetMiningSpeedResponse speedResponse = miningServiceGrpcClient.getMiningSpeed(userId);
            
            return MiningSpeedResponse.builder()
                    .userId(userId)
                    .baseSpeed(speedResponse.getBaseSpeed())
                    .referralBonus(speedResponse.getFriendBonus())
                    .statusBonus(speedResponse.getStatusBonus())
                    .stakingBonus(0.0) // TODO: Get from staking service
                    .serviceActivityBonus(speedResponse.getBookingBonus() + speedResponse.getVideoBoost())
                    .totalSpeed(speedResponse.getTotalSpeed())
                    .maxSpeed(5.0) // Max speed limit
                    .isActive(!speedResponse.getIsCapped())
                    .build();
        } catch (Exception e) {
            log.warn("Mining-service unavailable when getting mining speed for userId={}, returning default speed. Error: {}", userId, e.getMessage());
            // Return static default speed when mining-service is unavailable
            return MiningSpeedResponse.builder()
                    .userId(userId)
                    .baseSpeed(0.0)
                    .referralBonus(0.0)
                    .statusBonus(0.0)
                    .stakingBonus(0.0)
                    .serviceActivityBonus(0.0)
                    .totalSpeed(0.0)
                    .maxSpeed(5.0)
                    .isActive(false)
                    .build();
        }
    }

    @Override
    public com.tuttime.admin.dto.MiningStatusResponse getMiningStatus(Long userId) {
        try {
            log.debug("Getting mining status: userId={}", userId);
            
            Map<String, Object> statusData = miningServiceRestClient.getMiningStatus(userId);
            
            if (statusData == null || statusData.isEmpty()) {
                log.warn("Mining status data is empty for userId={}", userId);
                return buildDefaultMiningStatusResponse(userId);
            }
            
            log.debug("Mapping mining status data for userId={}", userId);
            return mapToMiningStatusResponse(statusData);
        } catch (Exception e) {
            log.error("Error getting mining status for userId={}: {}", userId, e.getMessage(), e);
            return buildDefaultMiningStatusResponse(userId);
        }
    }

    private com.tuttime.admin.dto.MiningStatusResponse mapToMiningStatusResponse(Map<String, Object> data) {
        Long userId = getLongValue(data, "userId");
        
        // Map balance fields - use BigDecimal directly to preserve decimal precision
        BigDecimal currentBalance = getBigDecimalValue(data, "currentBalance");
        BigDecimal monthlyAccumulated = getBigDecimalValue(data, "monthlyAccumulated");
        BigDecimal lifetimeAccumulated = getBigDecimalValue(data, "lifetimeAccumulated");
        BigDecimal dailyMinedApBalance = getBigDecimalValue(data, "dailyMinedApBalance");
        Boolean miningActive = getBooleanValue(data, "miningActive");
        BigDecimal totalApEarned = getBigDecimalValue(data, "totalApEarned");
        
        // Map speed breakdown
        @SuppressWarnings("unchecked")
        Map<String, Object> speedBreakdownData = (Map<String, Object>) data.get("speedBreakdown");
        com.tuttime.admin.dto.MiningSpeedBreakdownResponse speedBreakdown = null;
        if (speedBreakdownData != null) {
            speedBreakdown = com.tuttime.admin.dto.MiningSpeedBreakdownResponse.builder()
                    .totalSpeed(getBigDecimalValue(speedBreakdownData, "totalSpeed").doubleValue())
                    .baseSpeed(getBigDecimalValue(speedBreakdownData, "baseSpeed").doubleValue())
                    .friendBonus(getBigDecimalValue(speedBreakdownData, "friendBonus").doubleValue())
                    .bookingBonus(getBigDecimalValue(speedBreakdownData, "bookingBonus").doubleValue())
                    .rentalBonus(getBigDecimalValue(speedBreakdownData, "rentalBonus").doubleValue())
                    .videoBoost(getBigDecimalValue(speedBreakdownData, "videoBoost").doubleValue())
                    .taskBonus(getBigDecimalValue(speedBreakdownData, "taskBonus").doubleValue())
                    .statusBonus(getBigDecimalValue(speedBreakdownData, "statusBonus").doubleValue())
                    .stakingBonus(getBigDecimalValue(speedBreakdownData, "stakingBonus").doubleValue())
                    .streakBonus(getBigDecimalValue(speedBreakdownData, "streakBonus").doubleValue())
                    .isCapped(getBooleanValue(speedBreakdownData, "isCapped"))
                    .build();
        }
        
        return com.tuttime.admin.dto.MiningStatusResponse.builder()
                .userId(userId)
                .currentBalance(currentBalance != null ? currentBalance : BigDecimal.ZERO)
                .monthlyAccumulated(monthlyAccumulated != null ? monthlyAccumulated : BigDecimal.ZERO)
                .lifetimeAccumulated(lifetimeAccumulated != null ? lifetimeAccumulated : BigDecimal.ZERO)
                .dailyMinedApBalance(dailyMinedApBalance != null ? dailyMinedApBalance : BigDecimal.ZERO)
                .miningActive(miningActive)
                .speedBreakdown(speedBreakdown)
                .totalApEarned(totalApEarned != null ? totalApEarned : BigDecimal.ZERO)
                .sessionStartedAt(parseDateTime(data.get("sessionStartedAt")))
                .lastActivity(parseDateTime(data.get("lastActivity")))
                .lastUpdated(parseDateTime(data.get("lastUpdated")))
                .isPremiumAutoMining(getBooleanValue(data, "isPremiumAutoMining"))
                .autoMiningEnabledAt(parseDateTime(data.get("autoMiningEnabledAt")))
                .autoMiningExpiresAt(parseDateTime(data.get("autoMiningExpiresAt")))
                .build();
    }

    private com.tuttime.admin.dto.MiningStatusResponse buildDefaultMiningStatusResponse(Long userId) {
        return com.tuttime.admin.dto.MiningStatusResponse.builder()
                .userId(userId)
                .currentBalance(BigDecimal.ZERO)
                .monthlyAccumulated(BigDecimal.ZERO)
                .lifetimeAccumulated(BigDecimal.ZERO)
                .dailyMinedApBalance(BigDecimal.ZERO)
                .miningActive(false)
                .speedBreakdown(com.tuttime.admin.dto.MiningSpeedBreakdownResponse.builder()
                        .totalSpeed(0.0)
                        .baseSpeed(0.0)
                        .friendBonus(0.0)
                        .bookingBonus(0.0)
                        .rentalBonus(0.0)
                        .videoBoost(0.0)
                        .taskBonus(0.0)
                        .statusBonus(0.0)
                        .stakingBonus(0.0)
                        .streakBonus(0.0)
                        .isCapped(false)
                        .build())
                .totalApEarned(BigDecimal.ZERO)
                .sessionStartedAt(null)
                .lastActivity(null)
                .lastUpdated(OffsetDateTime.now(ZoneOffset.UTC))
                .isPremiumAutoMining(false)
                .autoMiningEnabledAt(null)
                .autoMiningExpiresAt(null)
                .build();
    }

}

