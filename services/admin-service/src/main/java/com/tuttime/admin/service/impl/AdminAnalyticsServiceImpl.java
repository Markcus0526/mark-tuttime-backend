package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.*;
// TODO: Uncomment when conversion-service is deployed
// import com.tuttime.admin.grpc.client.ConversionServiceGrpcClient;
import com.tuttime.admin.grpc.client.MiningServiceGrpcClient;
import com.tuttime.admin.grpc.client.RewardsServiceGrpcClient;
// TODO: Uncomment when staking-service is deployed
// import com.tuttime.admin.grpc.client.StakingServiceGrpcClient;
// TODO: Uncomment when token-service is deployed
// import com.tuttime.admin.grpc.client.TokenServiceGrpcClient;
import com.tuttime.admin.grpc.client.UserServiceGrpcClient;
import com.tuttime.admin.rest.client.MiningServiceRestClient;
import com.tuttime.admin.rest.client.ContentServiceRestClient;
import com.tuttime.admin.service.AdminAnalyticsService;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Admin Analytics Service Implementation
 * 
 * <p>Aggregates analytics data from all domain services via gRPC.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnalyticsServiceImpl implements AdminAnalyticsService {

    private final MiningServiceGrpcClient miningServiceGrpcClient;
    // TODO: Uncomment when conversion-service is deployed
    // private final ConversionServiceGrpcClient conversionServiceGrpcClient;
    private final UserServiceGrpcClient userServiceGrpcClient;
    // TODO: Uncomment when token-service is deployed
    // private final TokenServiceGrpcClient tokenServiceGrpcClient;
    // TODO: Uncomment when staking-service is deployed
    // private final StakingServiceGrpcClient stakingServiceGrpcClient;
    private final RewardsServiceGrpcClient rewardsServiceGrpcClient;
    private final MiningServiceRestClient miningServiceRestClient;
    private final ContentServiceRestClient contentServiceRestClient;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;
    private static final String REDIS_KEY_LAST_INACTIVE_NOTIFICATION = "mining:notification:inactive:last_sent_at";
    private static final String REDIS_KEY_LAST_NEVER_STARTED_NOTIFICATION = "mining:notification:never_started:last_sent_at";

    // TODO: should remove after debugging - gRPC call timeout in seconds
    private static final int GRPC_CALL_TIMEOUT_SECONDS = 10;

    @Override
    @Cacheable(value = "analytics", key = "'dashboard'", unless = "#result == null")
    public DashboardStatsResponse getDashboardStats() {
        // Note: If you get deserialization errors, clear the Redis cache:
        // redis-cli DEL analytics::dashboard
        // Or clear all analytics cache: redis-cli --scan --pattern "analytics::*" | xargs redis-cli DEL
        long startTime = System.currentTimeMillis(); // TODO: should remove after debugging
        log.debug("Fetching dashboard stats (cache key: analytics::dashboard)");
        
        try {
            // Parallelize independent gRPC calls to reduce total latency
            // All 6 original gRPC calls are parallelized:
            // 1. getUserStatistics() - user statistics (total, active, blocked users)
            // 2. getTotalMonthlyAccumulated() - total monthly AP credits
            // 3. getUsersByLevel() - users count by level (BRONZE, SILVER, GOLD, etc.)
            // 4. getActiveAutoMiningsCount() - count of active auto-minings
            // 5. getTotalReferralsCount() - total referrals count
            // 6. getActiveMiningSessionsCount() - active mining sessions count
            // TODO: should remove after debugging - log start of parallel calls
            log.debug("Starting parallel gRPC calls for dashboard stats");
            long parallelStartTime = System.currentTimeMillis(); // TODO: should remove after debugging

            // Start all 6 independent gRPC calls in parallel
            CompletableFuture<GetUserStatisticsResponse> userStatsFuture = CompletableFuture
                    .supplyAsync(() -> {
                        long callStart = System.currentTimeMillis(); // TODO: should remove after debugging
                        try {
                            log.debug("Calling getUserStatistics via gRPC (async)");
                            GetUserStatisticsResponse result = userServiceGrpcClient.getUserStatistics();
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.debug("getUserStatistics completed in {}ms", callDuration); // TODO: should remove after debugging
                            return result;
                        } catch (Exception e) {
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.error("getUserStatistics failed after {}ms", callDuration, e); // TODO: should remove after debugging
                            throw new RuntimeException("Failed to get user statistics", e);
                        }
                    })
                    .orTimeout(GRPC_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CompletableFuture<com.tuttime.common.proto.GetTotalMonthlyAccumulatedResponse> monthlyApFuture = CompletableFuture
                    .supplyAsync(() -> {
                        long callStart = System.currentTimeMillis(); // TODO: should remove after debugging
                        try {
                            log.debug("Calling getTotalMonthlyAccumulated via gRPC (async)");
                            com.tuttime.common.proto.GetTotalMonthlyAccumulatedResponse result = 
                                    miningServiceGrpcClient.getTotalMonthlyAccumulated();
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.debug("getTotalMonthlyAccumulated completed in {}ms", callDuration); // TODO: should remove after debugging
                            return result;
                        } catch (Exception e) {
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.error("getTotalMonthlyAccumulated failed after {}ms", callDuration, e); // TODO: should remove after debugging
                            throw new RuntimeException("Failed to get total monthly accumulated", e);
                        }
                    })
                    .orTimeout(GRPC_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CompletableFuture<Map<String, Long>> usersByLevelFuture = CompletableFuture
                    .supplyAsync(() -> {
                        long callStart = System.currentTimeMillis(); // TODO: should remove after debugging
                        try {
                            log.debug("Calling getUsersByLevel via gRPC (async)");
                            GetUsersByLevelResponse response = userServiceGrpcClient.getUsersByLevel();
                            Map<String, Long> result = response.getUsersByLevelMap();
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.debug("getUsersByLevel completed in {}ms", callDuration); // TODO: should remove after debugging
                            return result;
                        } catch (Exception e) {
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.warn("getUsersByLevel failed after {}ms, returning empty map", callDuration, e); // TODO: should remove after debugging
                            Map<String, Long> usersByLevel = new HashMap<>();
                            usersByLevel.put("BRONZE", 0L);
                            usersByLevel.put("SILVER", 0L);
                            usersByLevel.put("GOLD", 0L);
                            usersByLevel.put("PLATINUM", 0L);
                            usersByLevel.put("DIAMOND", 0L);
                            usersByLevel.put("ELITE", 0L);
                            return usersByLevel;
                        }
                    })
                    .orTimeout(GRPC_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CompletableFuture<Long> activeAutoMiningsFuture = CompletableFuture
                    .supplyAsync(() -> {
                        long callStart = System.currentTimeMillis(); // TODO: should remove after debugging
                        try {
                            log.debug("Calling getActiveAutoMiningsCount via gRPC (async)");
                            GetActiveAutoMiningsCountResponse response = miningServiceGrpcClient.getActiveAutoMiningsCount();
                            Long result = response.getCount();
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.debug("getActiveAutoMiningsCount completed in {}ms", callDuration); // TODO: should remove after debugging
                            return result;
                        } catch (Exception e) {
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.warn("getActiveAutoMiningsCount failed after {}ms, returning 0", callDuration, e); // TODO: should remove after debugging
                            return 0L;
                        }
                    })
                    .orTimeout(GRPC_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CompletableFuture<GetTotalReferralsCountResponse> totalReferralsFuture = CompletableFuture
                    .supplyAsync(() -> {
                        long callStart = System.currentTimeMillis(); // TODO: should remove after debugging
                        try {
                            log.debug("Calling getTotalReferralsCount via gRPC (async)");
                            GetTotalReferralsCountResponse result = rewardsServiceGrpcClient.getTotalReferralsCount();
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.debug("getTotalReferralsCount completed in {}ms", callDuration); // TODO: should remove after debugging
                            return result;
                        } catch (Exception e) {
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.error("getTotalReferralsCount failed after {}ms", callDuration, e); // TODO: should remove after debugging
                            throw new RuntimeException("Failed to get total referrals count", e);
                        }
                    })
                    .orTimeout(GRPC_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            CompletableFuture<GetActiveMiningSessionsCountResponse> activeSessionsFuture = CompletableFuture
                    .supplyAsync(() -> {
                        long callStart = System.currentTimeMillis(); // TODO: should remove after debugging
                        try {
                            log.debug("Calling getActiveMiningSessionsCount via gRPC (async)");
                            GetActiveMiningSessionsCountResponse result = miningServiceGrpcClient.getActiveMiningSessionsCount();
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.debug("getActiveMiningSessionsCount completed in {}ms", callDuration); // TODO: should remove after debugging
                            return result;
                        } catch (Exception e) {
                            long callDuration = System.currentTimeMillis() - callStart; // TODO: should remove after debugging
                            log.error("getActiveMiningSessionsCount failed after {}ms", callDuration, e); // TODO: should remove after debugging
                            throw new RuntimeException("Failed to get active mining sessions count", e);
                        }
                    })
                    .orTimeout(GRPC_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            // Wait for all parallel calls to complete (with overall timeout)
            // TODO: should remove after debugging - log parallel execution time
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    userStatsFuture, monthlyApFuture, usersByLevelFuture, 
                    activeAutoMiningsFuture, totalReferralsFuture, activeSessionsFuture
            );
            
            try {
                // Wait for all calls to complete with overall timeout (15 seconds for all calls)
                allFutures.get(15, TimeUnit.SECONDS);
                long parallelDuration = System.currentTimeMillis() - parallelStartTime; // TODO: should remove after debugging
                log.info("All parallel gRPC calls completed in {}ms", parallelDuration); // TODO: should remove after debugging
            } catch (TimeoutException e) {
                long parallelDuration = System.currentTimeMillis() - parallelStartTime; // TODO: should remove after debugging
                log.error("Parallel gRPC calls timed out after {}ms", parallelDuration, e); // TODO: should remove after debugging
                throw new BusinessException("Dashboard stats request timed out after " + parallelDuration + "ms");
            }

            // Get results from futures
            GetUserStatisticsResponse userStats = userStatsFuture.get();
            log.info("User statistics retrieved: totalUsers={}, activeUsers={}, blockedUsers={}", 
                    userStats.getTotalUsers(), userStats.getActiveUsers(), userStats.getBlockedUsers());

            com.tuttime.common.proto.GetTotalMonthlyAccumulatedResponse monthlyAp = monthlyApFuture.get();
            log.debug("Total monthly AP: {}", monthlyAp.getTotalMonthlyAccumulated());

            Map<String, Long> usersByLevel = usersByLevelFuture.get();
            Long activeAutoMinings = activeAutoMiningsFuture.get();
            GetTotalReferralsCountResponse totalReferrals = totalReferralsFuture.get();
            GetActiveMiningSessionsCountResponse activeSessions = activeSessionsFuture.get();
            
            // TODO: Uncomment when conversion-service is deployed
            // Get last conversion summary (don't fail if no conversions exist)
            ConversionSummaryResponse lastConversion = null;
            // try {
            //     lastConversion = getLastConversionSummary();
            // } catch (Exception e) {
            //     log.warn("Failed to get last conversion summary (may not exist yet), continuing without it", e);
            //     // Create empty conversion summary instead of failing
            //     lastConversion = ConversionSummaryResponse.builder()
            //             .totalUsers(0L)
            //             .totalApAmount(BigDecimal.ZERO)
            //             .averageUserShare(BigDecimal.ZERO)
            //             .averageDistributedApartAmount(BigDecimal.ZERO)
            //             .build();
            // }
            // Conversion service not deployed - use empty summary
            lastConversion = ConversionSummaryResponse.builder()
                    .totalUsers(0L)
                    .totalApAmount(BigDecimal.ZERO)
                    .averageUserShare(BigDecimal.ZERO)
                    .averageDistributedApartAmount(BigDecimal.ZERO)
                    .build();
            
            // TODO: Uncomment when staking-service is deployed
            // Get staking statistics
            // BigDecimal totalStakingApy = getTotalStakingApy();
            // BigDecimal totalRewardsClaimed = getTotalRewardsClaimed();
            // BigDecimal tvl = getTvl();
            // Staking service not deployed - use zero values
            BigDecimal totalStakingApy = BigDecimal.ZERO;
            BigDecimal totalRewardsClaimed = BigDecimal.ZERO;
            BigDecimal tvl = BigDecimal.ZERO;
            
            // TODO: Uncomment when token-service is deployed
            // Get total APART tokens
            // log.debug("Calling getTotalApartBalance via gRPC");
            // GetTotalApartBalanceResponse totalApart = tokenServiceGrpcClient.getTotalApartBalance();
            // log.debug("Total APART tokens: {}", totalApart.getTotalBalance());
            // Token service not deployed - use zero
            BigDecimal totalApartBalance = BigDecimal.ZERO;
            log.debug("Token service not deployed - using zero for total APART tokens");

            DashboardStatsResponse response = DashboardStatsResponse.builder()
                    .totalUsers(userStats.getTotalUsers())
                    .activeUsers(userStats.getActiveUsers())
                    .blockedUsers(userStats.getBlockedUsers())
                    .totalApCredits(new BigDecimal(monthlyAp.getTotalMonthlyAccumulated()))
                    .totalApartTokens(totalApartBalance) // Token service not deployed
                    .totalStaked(tvl) // Use TVL for total staked (staking service not deployed)
                    .totalReferrals(totalReferrals.getTotalReferrals())
                    .activeMiningSessions(activeSessions.getCount())
                    .usersByLevel(usersByLevel)
                    .activeAutoMinings(activeAutoMinings)
                    .lastConversion(lastConversion)
                    .totalStakingApy(totalStakingApy)
                    .totalRewardsClaimed(totalRewardsClaimed)
                    .tvl(tvl)
                    .build();
            
            long totalDuration = System.currentTimeMillis() - startTime; // TODO: should remove after debugging
            log.info("Dashboard stats built successfully in {}ms: totalUsers={}, totalApCredits={}, totalApartTokens={}", 
                    totalDuration, response.getTotalUsers(), response.getTotalApCredits(), response.getTotalApartTokens()); // TODO: should remove after debugging
            
            return response;
        } catch (BusinessException e) {
            long totalDuration = System.currentTimeMillis() - startTime; // TODO: should remove after debugging
            String stackTraceSummary = getStackTraceSummary(e, 3);
            log.error("Error getting dashboard stats after {}ms, stackTrace={}", totalDuration, stackTraceSummary); // TODO: should remove after debugging
            throw e;
        } catch (ExecutionException e) {
            long totalDuration = System.currentTimeMillis() - startTime; // TODO: should remove after debugging
            Throwable cause = e.getCause();
            Throwable errorToLog = cause != null ? cause : e;
            String stackTraceSummary = getStackTraceSummary(errorToLog, 3);
            log.error("Error getting dashboard stats after {}ms (execution exception), stackTrace={}", totalDuration, stackTraceSummary); // TODO: should remove after debugging
            throw new BusinessException("Failed to get dashboard stats: " + (cause != null ? cause.getMessage() : e.getMessage()));
        } catch (Exception e) {
            long totalDuration = System.currentTimeMillis() - startTime; // TODO: should remove after debugging
            String stackTraceSummary = getStackTraceSummary(e, 3);
            log.error("Error getting dashboard stats after {}ms, stackTrace={}", totalDuration, stackTraceSummary); // TODO: should remove after debugging
            throw new BusinessException("Failed to get dashboard stats: " + e.getMessage());
        }
    }
    
    /**
     * Gets a shortened stack trace summary for logging.
     * 
     * @param ex The exception
     * @param maxLines Maximum number of stack trace lines to include
     * @return Stack trace summary string (first N lines)
     */
    private String getStackTraceSummary(Throwable ex, int maxLines) {
        if (ex == null) {
            return "";
        }
        
        StackTraceElement[] stackTrace = ex.getStackTrace();
        if (stackTrace == null || stackTrace.length == 0) {
            return "";
        }
        
        StringBuilder summary = new StringBuilder();
        int linesToInclude = Math.min(maxLines, stackTrace.length);
        
        for (int i = 0; i < linesToInclude; i++) {
            if (i > 0) {
                summary.append(" | ");
            }
            summary.append(stackTrace[i].toString());
        }
        
        if (stackTrace.length > maxLines) {
            summary.append(" ... (").append(stackTrace.length - maxLines).append(" more)");
        }
        
        return summary.toString();
    }
    
    /**
     * Get users count by level (BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, ELITE)
     * 
     * @deprecated This method is deprecated. Users by level are now fetched in parallel 
     *             as part of getDashboardStats() optimization. This method is kept for 
     *             backward compatibility if needed elsewhere.
     */
    @Deprecated
    private Map<String, Long> getUsersByLevel() {
        long startTime = System.currentTimeMillis(); // TODO: should remove after debugging
        try {
            log.debug("Calling getUsersByLevel via gRPC (deprecated sequential call)"); // TODO: should remove after debugging
            GetUsersByLevelResponse response = userServiceGrpcClient.getUsersByLevel();
            long duration = System.currentTimeMillis() - startTime; // TODO: should remove after debugging
            log.debug("getUsersByLevel completed in {}ms (deprecated)", duration); // TODO: should remove after debugging
            return response.getUsersByLevelMap();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime; // TODO: should remove after debugging
            log.warn("Failed to get users by level after {}ms (deprecated), returning empty map", duration, e); // TODO: should remove after debugging
            Map<String, Long> usersByLevel = new HashMap<>();
            usersByLevel.put("BRONZE", 0L);
            usersByLevel.put("SILVER", 0L);
            usersByLevel.put("GOLD", 0L);
            usersByLevel.put("PLATINUM", 0L);
            usersByLevel.put("DIAMOND", 0L);
            usersByLevel.put("ELITE", 0L);
            return usersByLevel;
        }
    }
    
    /**
     * Get count of users with active auto-mining enabled
     * 
     * @deprecated This method is deprecated. Active auto-minings count is now fetched in parallel 
     *             as part of getDashboardStats() optimization. This method is kept for 
     *             backward compatibility if needed elsewhere.
     */
    @Deprecated
    private Long getActiveAutoMinings() {
        long startTime = System.currentTimeMillis(); // TODO: should remove after debugging
        try {
            log.debug("Calling getActiveAutoMiningsCount via gRPC (deprecated sequential call)"); // TODO: should remove after debugging
            GetActiveAutoMiningsCountResponse response = miningServiceGrpcClient.getActiveAutoMiningsCount();
            long duration = System.currentTimeMillis() - startTime; // TODO: should remove after debugging
            log.debug("getActiveAutoMiningsCount completed in {}ms (deprecated)", duration); // TODO: should remove after debugging
            return response.getCount();
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime; // TODO: should remove after debugging
            log.warn("Failed to get active auto-minings count after {}ms (deprecated)", duration, e); // TODO: should remove after debugging
            return 0L;
        }
    }
    
    // TODO: Uncomment when staking-service is deployed
    /**
     * Get total staking APY (weighted average)
     */
    /*
    private BigDecimal getTotalStakingApy() {
        try {
            GetTotalStakingApyResponse response = stakingServiceGrpcClient.getTotalStakingApy();
            return new BigDecimal(response.getTotalApy());
        } catch (Exception e) {
            log.warn("Failed to get total staking APY", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Get total rewards claimed from staking
     */
    /*
    private BigDecimal getTotalRewardsClaimed() {
        try {
            GetTotalRewardsClaimedResponse response = stakingServiceGrpcClient.getTotalRewardsClaimed();
            return new BigDecimal(response.getTotalRewardsClaimed());
        } catch (Exception e) {
            log.warn("Failed to get total rewards claimed", e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * Get TVL (Total Value Locked) - total staked amount
     */
    /*
    private BigDecimal getTvl() {
        try {
            GetTvlResponse response = stakingServiceGrpcClient.getTvl();
            return new BigDecimal(response.getTvl());
        } catch (Exception e) {
            log.warn("Failed to get TVL", e);
            return BigDecimal.ZERO;
        }
    }
    */

    @Override
    public UserStatsResponse getUserStats(LocalDate startDate, LocalDate endDate) {
        // TODO: Implement user stats aggregation
        return UserStatsResponse.builder()
                .totalUsers(0L)
                .newUsers(0L)
                .activeUsers(0L)
                .blockedUsers(0L)
                .dailyGrowth(new ArrayList<>())
                .build();
    }

    @Override
    public MiningStatsResponse getMiningStats(LocalDate startDate, LocalDate endDate) {
        try {
            com.tuttime.common.proto.GetTotalMonthlyAccumulatedResponse monthlyAp = 
                    miningServiceGrpcClient.getTotalMonthlyAccumulated();

            // Get total user count
            long totalUsers = 0L;
            try {
                com.tuttime.common.proto.GetUserStatisticsResponse userStats = 
                        userServiceGrpcClient.getUserStatistics();
                totalUsers = userStats.getTotalUsers();
                log.debug("Retrieved total user count: {}", totalUsers);
            } catch (Exception e) {
                log.warn("Error getting total user count: {}", e.getMessage());
                // Continue with 0 if user service is unavailable
            }

            // Calculate active and inactive miner counts from AP balances
            long activeMinerCount = 0L;
            long inactiveMinerCount = 0L;
            
            try {
                // Fetch AP balances in batches to count active/inactive miners
                int pageSize = 1000;
                int currentPage = 0;
                boolean hasMore = true;
                
                while (hasMore) {
                    Pageable pageable = PageRequest.of(currentPage, pageSize);
                    Page<Map<String, Object>> balancesPage = miningServiceRestClient.listApBalances(pageable);
                    
                    if (balancesPage.isEmpty()) {
                        hasMore = false;
                        break;
                    }
                    
                    List<Map<String, Object>> balances = balancesPage.getContent();
                    
                    for (Map<String, Object> balance : balances) {
                        Object miningActiveObj = balance.get("miningActive");
                        Object lifetimeAccumulatedObj = balance.get("lifetimeAccumulated");
                        
                        // Parse miningActive
                        Boolean miningActive = null;
                        if (miningActiveObj instanceof Boolean) {
                            miningActive = (Boolean) miningActiveObj;
                        } else if (miningActiveObj != null) {
                            String miningActiveStr = miningActiveObj.toString().toLowerCase();
                            if ("true".equals(miningActiveStr) || "1".equals(miningActiveStr)) {
                                miningActive = true;
                            } else {
                                miningActive = false;
                            }
                        } else {
                            // null means no mining active status, treat as inactive
                            miningActive = false;
                        }
                        
                        // Parse lifetimeAccumulated to determine if user has ever mined
                        BigDecimal lifetimeAccumulated = BigDecimal.ZERO;
                        if (lifetimeAccumulatedObj != null) {
                            if (lifetimeAccumulatedObj instanceof BigDecimal) {
                                lifetimeAccumulated = (BigDecimal) lifetimeAccumulatedObj;
                            } else if (lifetimeAccumulatedObj instanceof Number) {
                                lifetimeAccumulated = new BigDecimal(lifetimeAccumulatedObj.toString());
                            } else {
                                try {
                                    lifetimeAccumulated = new BigDecimal(lifetimeAccumulatedObj.toString());
                                } catch (NumberFormatException e) {
                                    lifetimeAccumulated = BigDecimal.ZERO;
                                }
                            }
                        }
                        
                        // Categorize users:
                        // - Active miners: miningActive = true
                        // - Inactive miners (mined before): miningActive = false/null AND lifetimeAccumulated > 0
                        if (Boolean.TRUE.equals(miningActive)) {
                            activeMinerCount++;
                        } else {
                            // Check if they have ever mined (lifetimeAccumulated > 0.001 to account for rounding)
                            if (lifetimeAccumulated.compareTo(new BigDecimal("0.001")) > 0) {
                                inactiveMinerCount++;
                            }
                            // Note: Users with lifetimeAccumulated = 0 are counted in neverStartedMiningCount
                            // which is calculated as: totalUsers - activeMinerCount - inactiveMinerCount
                        }
                    }
                    
                    // Check if there are more pages
                    if (balancesPage.isLast()) {
                        hasMore = false;
                    } else {
                        currentPage++;
                    }
                }
                
                log.debug("Calculated miner counts: active={}, inactive={}", 
                        activeMinerCount, inactiveMinerCount);
            } catch (Exception e) {
                log.warn("Error calculating miner counts from AP balances: {}", e.getMessage());
                // Continue with default values (0) if calculation fails
            }
            
            // Calculate never started mining count: totalUsers - activeMinerCount - inactiveMinerCount
            long neverStartedMiningCount = Math.max(0L, totalUsers - activeMinerCount - inactiveMinerCount);
            log.debug("Calculated never started mining count: totalUsers={}, active={}, inactive={}, neverStarted={}", 
                    totalUsers, activeMinerCount, inactiveMinerCount, neverStartedMiningCount);
            
            // Get last notification timestamps from Redis
            OffsetDateTime lastInactiveNotificationSentAt = null;
            OffsetDateTime lastNeverStartedNotificationSentAt = null;
            OffsetDateTime lastNotificationSentAt = null; // Deprecated: kept for backward compatibility
            
            if (redisTemplate != null) {
                try {
                    // Get inactive notification timestamp (new key)
                    String inactiveTimestampStr = redisTemplate.opsForValue().get(REDIS_KEY_LAST_INACTIVE_NOTIFICATION);
                    if (inactiveTimestampStr != null && !inactiveTimestampStr.isEmpty()) {
                        lastInactiveNotificationSentAt = OffsetDateTime.parse(inactiveTimestampStr);
                        lastNotificationSentAt = lastInactiveNotificationSentAt; // For backward compatibility
                        log.debug("Retrieved last inactive notification timestamp from Redis: {}", lastInactiveNotificationSentAt);
                    } else {
                        // Fallback to old key for backward compatibility
                        String oldKeyTimestampStr = redisTemplate.opsForValue().get("mining:notification:last_sent_at");
                        if (oldKeyTimestampStr != null && !oldKeyTimestampStr.isEmpty()) {
                            lastInactiveNotificationSentAt = OffsetDateTime.parse(oldKeyTimestampStr);
                            lastNotificationSentAt = lastInactiveNotificationSentAt;
                            log.debug("Retrieved last inactive notification timestamp from old Redis key (migrated): {}", lastInactiveNotificationSentAt);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error retrieving last inactive notification timestamp from Redis: {}", e.getMessage());
                }
                
                try {
                    // Get never-started notification timestamp
                    String neverStartedTimestampStr = redisTemplate.opsForValue().get(REDIS_KEY_LAST_NEVER_STARTED_NOTIFICATION);
                    if (neverStartedTimestampStr != null && !neverStartedTimestampStr.isEmpty()) {
                        lastNeverStartedNotificationSentAt = OffsetDateTime.parse(neverStartedTimestampStr);
                        log.debug("Retrieved last never-started notification timestamp from Redis: {}", lastNeverStartedNotificationSentAt);
                    }
                } catch (Exception e) {
                    log.warn("Error retrieving last never-started notification timestamp from Redis: {}", e.getMessage());
                }
            }

            return MiningStatsResponse.builder()
                    .totalApCredits(new BigDecimal(monthlyAp.getTotalMonthlyAccumulated()))
                    .monthlyApCredits(new BigDecimal(monthlyAp.getTotalMonthlyAccumulated()))
                    .activeMiningSessions(0L) // TODO: Get from mining-service
                    .activeMinerCount(activeMinerCount)
                    .inactiveMinerCount(inactiveMinerCount)
                    .neverStartedMiningCount(neverStartedMiningCount)
                    .averageMiningSpeed(BigDecimal.valueOf(1.0)) // TODO: Calculate average
                    .lastNotificationSentAt(lastNotificationSentAt) // Deprecated: kept for backward compatibility
                    .lastInactiveNotificationSentAt(lastInactiveNotificationSentAt)
                    .lastNeverStartedNotificationSentAt(lastNeverStartedNotificationSentAt)
                    .dailyStats(new ArrayList<>())
                    .build();
        } catch (Exception e) {
            log.error("Error getting mining stats", e);
            throw new BusinessException("Failed to get mining stats: " + e.getMessage());
        }
    }

    @Override
    public TokenStatsResponse getTokenStats() {
        // TODO: Implement token stats aggregation
        return TokenStatsResponse.builder()
                .totalSupply(BigDecimal.ZERO)
                .totalCirculating(BigDecimal.ZERO)
                .totalStaked(BigDecimal.ZERO)
                .totalBurned(BigDecimal.ZERO)
                .totalHolders(0L)
                .activeStakers(0L)
                .build();
    }

    @Override
    public ConversionStatsResponse getConversionStats() {
        // TODO: Implement conversion stats aggregation
        return ConversionStatsResponse.builder()
                .totalConverted(BigDecimal.ZERO)
                .lastMonthConverted(BigDecimal.ZERO)
                .thisMonthForecast(BigDecimal.ZERO)
                .monthlyConversions(new ArrayList<>())
                .build();
    }
    
    @Override
    public List<HistoricalDataPoint> getApGrowthHistory(Period period) {
        try {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime startTime = calculateStartTime(now, period);
            
            GetApGrowthHistoryResponse response = miningServiceGrpcClient.getApGrowthHistory(
                    startTime.format(ISO_FORMATTER),
                    now.format(ISO_FORMATTER)
            );
            
            return response.getDataPointsList().stream()
                    .map(dp -> HistoricalDataPoint.builder()
                            .timestamp(OffsetDateTime.parse(dp.getTimestamp(), ISO_FORMATTER))
                            .value(new BigDecimal(dp.getApAmount()))
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting AP growth history for period: {}", period, e);
            throw new BusinessException("Failed to get AP growth history: " + e.getMessage());
        }
    }
    
    // TODO: Uncomment when token-service is deployed
    @Override
    public List<HistoricalDataPoint> getApartGrowthHistory(Period period) {
        // try {
        //     OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        //     OffsetDateTime startTime = calculateStartTime(now, period);
        //     
        //     GetApartGrowthHistoryResponse response = tokenServiceGrpcClient.getApartGrowthHistory(
        //             startTime.format(ISO_FORMATTER),
        //             now.format(ISO_FORMATTER)
        //     );
        //     
        //     // Handle empty responses gracefully (e.g., when service is unavailable)
        //     if (response == null || response.getDataPointsList() == null || response.getDataPointsList().isEmpty()) {
        //         log.warn("Empty APART growth history response for period: {}, returning empty list", period);
        //         return Collections.emptyList();
        //     }
        //     
        //     return response.getDataPointsList().stream()
        //             .map(dp -> HistoricalDataPoint.builder()
        //                     .timestamp(OffsetDateTime.parse(dp.getTimestamp(), ISO_FORMATTER))
        //                     .value(new BigDecimal(dp.getApartAmount()))
        //                     .build())
        //             .collect(Collectors.toList());
        // } catch (Exception e) {
        //     log.error("Error getting APART growth history for period: {}", period, e);
        //     // Return empty list instead of throwing exception to allow dashboard to render
        //     return Collections.emptyList();
        // }
        // Token service not deployed - return empty list
        log.warn("Token service not deployed - returning empty APART growth history");
        return Collections.emptyList();
    }
    
    // TODO: Uncomment when conversion-service is deployed
    @Override
    public ConversionSummaryResponse getLastConversionSummary() {
        // try {
        //     log.debug("Calling getLastConversionSummary via gRPC");
        //     GetLastConversionSummaryResponse response = conversionServiceGrpcClient.getLastConversionSummary();
        //     
        //     // Check if conversion exists (conversionId > 0 indicates a real conversion)
        //     if (response.getConversionId() == 0) {
        //         log.debug("No conversion found, returning empty summary");
        //         return ConversionSummaryResponse.builder()
        //                 .totalUsers(0L)
        //                 .totalApAmount(BigDecimal.ZERO)
        //                 .averageUserShare(BigDecimal.ZERO)
        //                 .averageDistributedApartAmount(BigDecimal.ZERO)
        //                 .build();
        //     }
        //     
        //     OffsetDateTime conversionDate = null;
        //     if (response.getConversionDate() != null && !response.getConversionDate().isEmpty()) {
        //         try {
        //             conversionDate = OffsetDateTime.parse(response.getConversionDate(), ISO_FORMATTER);
        //         } catch (Exception e) {
        //             log.warn("Failed to parse conversion date: {}", response.getConversionDate(), e);
        //         }
        //     }
        //     
        //     ConversionSummaryResponse summary = ConversionSummaryResponse.builder()
        //             .totalUsers(response.getTotalUsersCount())  // Map to totalUsers for API response
        //             .totalApAmount(new BigDecimal(response.getTotalApAmount()))
        //             .averageUserShare(new BigDecimal(response.getAverageUserShare()))
        //             .averageDistributedApartAmount(new BigDecimal(response.getAverageDistributedApart()))  // Map to averageDistributedApartAmount
        //             .conversionDate(conversionDate)
        //             .conversionId(response.getConversionId() > 0 ? response.getConversionId() : null)
        //             .build();
        //     
        //     log.debug("Last conversion summary retrieved: conversionId={}, totalUsers={}, totalApAmount={}", 
        //             summary.getConversionId(), summary.getTotalUsers(), summary.getTotalApAmount());
        //     
        //     return summary;
        // } catch (Exception e) {
        //     log.error("Error getting last conversion summary", e);
        //     // Don't throw exception - return empty summary instead
        //     // This allows dashboard to render even if no conversions exist
        //     log.warn("Returning empty conversion summary due to error");
        //     return ConversionSummaryResponse.builder()
        //             .totalUsers(0L)
        //             .totalApAmount(BigDecimal.ZERO)
        //             .averageUserShare(BigDecimal.ZERO)
        //             .averageDistributedApartAmount(BigDecimal.ZERO)
        //             .build();
        // }
        // Conversion service not deployed - return empty summary
        log.warn("Conversion service not deployed - returning empty conversion summary");
        return ConversionSummaryResponse.builder()
                .totalUsers(0L)
                .totalApAmount(BigDecimal.ZERO)
                .averageUserShare(BigDecimal.ZERO)
                .averageDistributedApartAmount(BigDecimal.ZERO)
                .build();
    }
    
    /**
     * Calculate start time based on period
     */
    private OffsetDateTime calculateStartTime(OffsetDateTime now, Period period) {
        if (period == null) {
            throw new IllegalArgumentException("Period cannot be null");
        }
        
        if (period == Period.TODAY) {
            return now.truncatedTo(ChronoUnit.DAYS);
        } else if (period == Period.YESTERDAY) {
            return now.truncatedTo(ChronoUnit.DAYS).minusDays(1);
        } else if (period == Period.WEEK) {
            return now.minusWeeks(1);
        } else if (period == Period.MONTH) {
            return now.minusMonths(1);
        } else if (period == Period.YEAR) {
            return now.minusYears(1);
        } else if (period == Period.TOTAL) {
            return OffsetDateTime.of(2020, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC); // System start date
        } else {
            throw new IllegalArgumentException("Unknown period: " + period);
        }
    }
    
    @Override
    public RegistrationStatsResponse getRegistrationsPerDay(LocalDate startDate, LocalDate endDate) {
        try {
            // Default to last 30 days if not specified
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            GetRegistrationsPerDayResponse response = userServiceGrpcClient.getRegistrationsPerDay(startDate, endDate);
            
            List<RegistrationStatsResponse.DailyRegistrationStat> dailyStats = response.getStatsList().stream()
                    .map(stat -> RegistrationStatsResponse.DailyRegistrationStat.builder()
                            .date(LocalDate.parse(stat.getDate()))
                            .count(stat.getCount())
                            .build())
                    .collect(Collectors.toList());
            
            return RegistrationStatsResponse.builder()
                    .dailyStats(dailyStats)
                    .monthlyStats(Collections.emptyList())
                    .build();
        } catch (Exception e) {
            log.error("Error getting registrations per day", e);
            throw new BusinessException("Failed to get registrations per day: " + e.getMessage());
        }
    }
    
    @Override
    public RegistrationStatsResponse getRegistrationsPerMonth(LocalDate startDate, LocalDate endDate) {
        try {
            // Default to last 12 months if not specified
            if (startDate == null) {
                startDate = LocalDate.now().minusMonths(12).withDayOfMonth(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            GetRegistrationsPerMonthResponse response = userServiceGrpcClient.getRegistrationsPerMonth(startDate, endDate);
            
            List<RegistrationStatsResponse.MonthlyRegistrationStat> monthlyStats = response.getStatsList().stream()
                    .map(stat -> RegistrationStatsResponse.MonthlyRegistrationStat.builder()
                            .month(stat.getMonth())
                            .count(stat.getCount())
                            .build())
                    .collect(Collectors.toList());
            
            return RegistrationStatsResponse.builder()
                    .dailyStats(Collections.emptyList())
                    .monthlyStats(monthlyStats)
                    .build();
        } catch (Exception e) {
            log.error("Error getting registrations per month", e);
            throw new BusinessException("Failed to get registrations per month: " + e.getMessage());
        }
    }
    
    @Override
    public TaskCompletionStatsResponse getPopularTasksPerDay(LocalDate startDate, LocalDate endDate) {
        try {
            // Default to last 30 days if not specified
            if (startDate == null) {
                startDate = LocalDate.now().minusDays(30);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            List<Map<String, Object>> rawStats = contentServiceRestClient.getPopularTasksPerDay(startDate, endDate);
            
            List<TaskCompletionStatsResponse.DailyTaskStat> dailyStats = rawStats.stream()
                    .map(stat -> {
                        Object taskIdObj = stat.get("taskId");
                        Object taskTitleObj = stat.get("taskTitle");
                        Object dateObj = stat.get("date");
                        Object countObj = stat.get("completionCount");
                        
                        Long taskId = taskIdObj != null ? 
                                (taskIdObj instanceof Number ? ((Number) taskIdObj).longValue() : Long.parseLong(taskIdObj.toString())) : null;
                        String taskTitle = taskTitleObj != null ? taskTitleObj.toString() : null;
                        LocalDate date = dateObj != null ? LocalDate.parse(dateObj.toString()) : null;
                        Long count = countObj != null ? 
                                (countObj instanceof Number ? ((Number) countObj).longValue() : Long.parseLong(countObj.toString())) : 0L;
                        
                        return TaskCompletionStatsResponse.DailyTaskStat.builder()
                                .date(date)
                                .taskId(taskId)
                                .taskTitle(taskTitle)
                                .completionCount(count)
                                .build();
                    })
                    .collect(Collectors.toList());
            
            return TaskCompletionStatsResponse.builder()
                    .dailyStats(dailyStats)
                    .monthlyStats(Collections.emptyList())
                    .build();
        } catch (Exception e) {
            log.error("Error getting popular tasks per day", e);
            throw new BusinessException("Failed to get popular tasks per day: " + e.getMessage());
        }
    }
    
    @Override
    public TaskCompletionStatsResponse getPopularTasksPerMonth(LocalDate startDate, LocalDate endDate) {
        try {
            // Default to last 12 months if not specified
            if (startDate == null) {
                startDate = LocalDate.now().minusMonths(12).withDayOfMonth(1);
            }
            if (endDate == null) {
                endDate = LocalDate.now();
            }
            
            List<Map<String, Object>> rawStats = contentServiceRestClient.getPopularTasksPerMonth(startDate, endDate);
            
            List<TaskCompletionStatsResponse.MonthlyTaskStat> monthlyStats = rawStats.stream()
                    .map(stat -> {
                        Object taskIdObj = stat.get("taskId");
                        Object taskTitleObj = stat.get("taskTitle");
                        Object monthObj = stat.get("month");
                        Object countObj = stat.get("completionCount");
                        
                        Long taskId = taskIdObj != null ? 
                                (taskIdObj instanceof Number ? ((Number) taskIdObj).longValue() : Long.parseLong(taskIdObj.toString())) : null;
                        String taskTitle = taskTitleObj != null ? taskTitleObj.toString() : null;
                        String month = monthObj != null ? monthObj.toString() : null;
                        Long count = countObj != null ? 
                                (countObj instanceof Number ? ((Number) countObj).longValue() : Long.parseLong(countObj.toString())) : 0L;
                        
                        return TaskCompletionStatsResponse.MonthlyTaskStat.builder()
                                .month(month)
                                .taskId(taskId)
                                .taskTitle(taskTitle)
                                .completionCount(count)
                                .build();
                    })
                    .collect(Collectors.toList());
            
            return TaskCompletionStatsResponse.builder()
                    .dailyStats(Collections.emptyList())
                    .monthlyStats(monthlyStats)
                    .build();
        } catch (Exception e) {
            log.error("Error getting popular tasks per month", e);
            throw new BusinessException("Failed to get popular tasks per month: " + e.getMessage());
        }
    }
}

