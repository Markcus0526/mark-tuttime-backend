package com.tuttime.user.grpc.client;

import com.tuttime.common.proto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC Client for Staking Service
 *
 * <p>This client provides methods to interact with Staking Service via gRPC.
 * Includes circuit breaker and retry mechanisms for resilience.
 */
@Slf4j
@Component
public class StakingServiceGrpcClient {

    @GrpcClient("staking-service")
    private StakingServiceGrpc.StakingServiceBlockingStub stakingServiceStub;

    /**
     * Gets staking positions for a user.
     *
     * @param userId The user ID
     * @return StakingPositionsResponse containing staking positions
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getStakingPositionsFallback")
    @Retry(name = "grpc")
    public StakingPositionsResponse getStakingPositions(Long userId) {
        log.debug("gRPC: Calling Staking Service GetStakingPositions for userId: {}", userId);
        try {
            GetStakingPositionsRequest request = GetStakingPositionsRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return stakingServiceStub.getStakingPositions(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting staking positions for user: {}", userId, e);
            throw new RuntimeException("Failed to get staking positions: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private StakingPositionsResponse getStakingPositionsFallback(Long userId, Exception e) {
        log.warn("Circuit breaker open, returning empty staking positions for user: {}", userId);
        return StakingPositionsResponse.newBuilder().build();
    }
}

