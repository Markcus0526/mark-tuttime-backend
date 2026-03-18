package com.tuttime.admin.grpc.client;

import com.tuttime.common.proto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC Client for Staking Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StakingServiceGrpcClient {

    @GrpcClient("staking-service")
    private StakingServiceGrpc.StakingServiceBlockingStub stakingServiceStub;

    @CircuitBreaker(name = "grpc", fallbackMethod = "getStakingPositionsFallback")
    @Retry(name = "grpc")
    public StakingPositionsResponse getStakingPositions(Long userId) {
        try {
            GetStakingPositionsRequest request = GetStakingPositionsRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return stakingServiceStub.getStakingPositions(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting staking positions: userId={}", userId, e);
            throw new RuntimeException("Failed to get staking positions: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "grpc", fallbackMethod = "calculateRewardsFallback")
    @Retry(name = "grpc")
    public CalculateRewardsResponse calculateRewards(Long userId) {
        try {
            CalculateRewardsRequest request = CalculateRewardsRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return stakingServiceStub.calculateRewards(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error calculating rewards: userId={}", userId, e);
            throw new RuntimeException("Failed to calculate rewards: " + e.getMessage(), e);
        }
    }

    // Fallback methods
    private StakingPositionsResponse getStakingPositionsFallback(Long userId, Exception e) {
        log.warn("Circuit breaker open, returning empty positions for user: {}", userId);
        return StakingPositionsResponse.newBuilder().build();
    }

    private CalculateRewardsResponse calculateRewardsFallback(Long userId, Exception e) {
        log.warn("Circuit breaker open, returning zero rewards");
        return CalculateRewardsResponse.newBuilder()
                .setTotalRewards("0")
                .setSuccess(false)
                .build();
    }

    /**
     * Get total staking APY (weighted average)
     * 
     * NOTE: This endpoint needs to be added to staking-service proto:
     * rpc GetTotalStakingApy (GetTotalStakingApyRequest) returns (GetTotalStakingApyResponse);
     * 
     * TODO: Add GetTotalStakingApy endpoint to staking_service.proto
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getTotalStakingApyFallback")
    @Retry(name = "grpc")
    public GetTotalStakingApyResponse getTotalStakingApy() {
        try {
            GetTotalStakingApyRequest request = GetTotalStakingApyRequest.newBuilder().build();
            return stakingServiceStub.getTotalStakingApy(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
                log.debug("getTotalStakingApy not yet implemented in staking-service, using fallback");
                return getTotalStakingApyFallback(e);
            }
            log.error("gRPC error getting total staking APY", e);
            throw new RuntimeException("Failed to get total staking APY: " + e.getMessage(), e);
        }
    }

    /**
     * Get total rewards claimed from staking
     * 
     * NOTE: This endpoint needs to be added to staking-service proto:
     * rpc GetTotalRewardsClaimed (GetTotalRewardsClaimedRequest) returns (GetTotalRewardsClaimedResponse);
     * 
     * TODO: Add GetTotalRewardsClaimed endpoint to staking_service.proto
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getTotalRewardsClaimedFallback")
    @Retry(name = "grpc")
    public GetTotalRewardsClaimedResponse getTotalRewardsClaimed() {
        try {
            GetTotalRewardsClaimedRequest request = GetTotalRewardsClaimedRequest.newBuilder().build();
            return stakingServiceStub.getTotalRewardsClaimed(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
                log.debug("getTotalRewardsClaimed not yet implemented in staking-service, using fallback");
                return getTotalRewardsClaimedFallback(e);
            }
            log.error("gRPC error getting total rewards claimed", e);
            throw new RuntimeException("Failed to get total rewards claimed: " + e.getMessage(), e);
        }
    }

    /**
     * Get TVL (Total Value Locked) - total staked amount
     * 
     * NOTE: This endpoint needs to be added to staking-service proto:
     * rpc GetTvl (GetTvlRequest) returns (GetTvlResponse);
     * 
     * TODO: Add GetTvl endpoint to staking_service.proto
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getTvlFallback")
    @Retry(name = "grpc")
    public GetTvlResponse getTvl() {
        try {
            GetTvlRequest request = GetTvlRequest.newBuilder().build();
            return stakingServiceStub.getTvl(request);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
                log.debug("getTvl not yet implemented in staking-service, using fallback");
                return getTvlFallback(e);
            }
            log.error("gRPC error getting TVL", e);
            throw new RuntimeException("Failed to get TVL: " + e.getMessage(), e);
        }
    }

    // Fallback methods for new endpoints
    private GetTotalStakingApyResponse getTotalStakingApyFallback(Exception e) {
        log.warn("Circuit breaker open or endpoint not implemented, returning zero APY");
        return GetTotalStakingApyResponse.newBuilder()
                .setTotalApy("0")
                .build();
    }

    private GetTotalRewardsClaimedResponse getTotalRewardsClaimedFallback(Exception e) {
        log.warn("Circuit breaker open or endpoint not implemented, returning zero rewards");
        return GetTotalRewardsClaimedResponse.newBuilder()
                .setTotalRewardsClaimed("0")
                .build();
    }

    private GetTvlResponse getTvlFallback(Exception e) {
        log.warn("Circuit breaker open or endpoint not implemented, returning zero TVL");
        return GetTvlResponse.newBuilder()
                .setTvl("0")
                .build();
    }

    /**
     * Get top staking positions
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getTopStakingsFallback")
    @Retry(name = "grpc")
    public GetTopStakingsResponse getTopStakings(String sortBy, Long poolId, Integer limit) {
        try {
            GetTopStakingsRequest.Builder requestBuilder = GetTopStakingsRequest.newBuilder()
                    .setSortBy(sortBy != null ? sortBy : "apy")
                    .setLimit(limit != null ? limit : 100);
            if (poolId != null && poolId > 0) {
                requestBuilder.setPoolId(poolId);
            }
            return stakingServiceStub.getTopStakings(requestBuilder.build());
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
                log.debug("getTopStakings not yet implemented in staking-service, using fallback");
                return getTopStakingsFallback(sortBy, poolId, limit, e);
            }
            log.error("gRPC error getting top stakings", e);
            throw new RuntimeException("Failed to get top stakings: " + e.getMessage(), e);
        }
    }

    // Fallback method for leaderboard endpoint
    private GetTopStakingsResponse getTopStakingsFallback(String sortBy, Long poolId, Integer limit, Exception e) {
        log.warn("Circuit breaker open or endpoint not implemented, returning empty list");
        return GetTopStakingsResponse.newBuilder().build();
    }

    @CircuitBreaker(name = "grpc", fallbackMethod = "getStakingHistoryFallback")
    @Retry(name = "grpc")
    public GetStakingHistoryResponse getStakingHistory(Long poolId, Long userId, String startDate, String endDate, int page, int size) {
        try {
            GetStakingHistoryRequest.Builder requestBuilder = GetStakingHistoryRequest.newBuilder()
                    .setPage(page)
                    .setSize(size);
            if (poolId != null && poolId > 0) {
                requestBuilder.setPoolId(poolId);
            }
            if (userId != null && userId > 0) {
                requestBuilder.setUserId(userId);
            }
            if (startDate != null && !startDate.isEmpty()) {
                requestBuilder.setStartDate(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                requestBuilder.setEndDate(endDate);
            }
            return stakingServiceStub.getStakingHistory(requestBuilder.build());
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting staking history", e);
            throw new RuntimeException("Failed to get staking history: " + e.getMessage(), e);
        }
    }

    @CircuitBreaker(name = "grpc", fallbackMethod = "getUnstakingHistoryFallback")
    @Retry(name = "grpc")
    public GetUnstakingHistoryResponse getUnstakingHistory(Long poolId, Long userId, String startDate, String endDate, int page, int size) {
        try {
            GetUnstakingHistoryRequest.Builder requestBuilder = GetUnstakingHistoryRequest.newBuilder()
                    .setPage(page)
                    .setSize(size);
            if (poolId != null && poolId > 0) {
                requestBuilder.setPoolId(poolId);
            }
            if (userId != null && userId > 0) {
                requestBuilder.setUserId(userId);
            }
            if (startDate != null && !startDate.isEmpty()) {
                requestBuilder.setStartDate(startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                requestBuilder.setEndDate(endDate);
            }
            return stakingServiceStub.getUnstakingHistory(requestBuilder.build());
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting unstaking history", e);
            throw new RuntimeException("Failed to get unstaking history: " + e.getMessage(), e);
        }
    }

    // Fallback methods
    private GetStakingHistoryResponse getStakingHistoryFallback(Long poolId, Long userId, String startDate, String endDate, int page, int size, Exception e) {
        log.warn("Circuit breaker open, returning empty staking history");
        return GetStakingHistoryResponse.newBuilder()
                .setTotalPages(0)
                .setTotalElements(0)
                .setHasNext(false)
                .build();
    }

    private GetUnstakingHistoryResponse getUnstakingHistoryFallback(Long poolId, Long userId, String startDate, String endDate, int page, int size, Exception e) {
        log.warn("Circuit breaker open, returning empty unstaking history");
        return GetUnstakingHistoryResponse.newBuilder()
                .setTotalPages(0)
                .setTotalElements(0)
                .setHasNext(false)
                .build();
    }
}

