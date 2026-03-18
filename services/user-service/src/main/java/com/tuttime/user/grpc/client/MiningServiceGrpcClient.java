package com.tuttime.user.grpc.client;

import com.tuttime.common.proto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC Client for Mining Service
 *
 * <p>This client provides methods to interact with Mining Service via gRPC.
 * Includes circuit breaker and retry mechanisms for resilience.
 */
@Slf4j
@Component
public class MiningServiceGrpcClient {

    @GrpcClient("mining-service")
    private MiningServiceGrpc.MiningServiceBlockingStub miningServiceStub;

    /**
     * Gets the AP balance for a user.
     *
     * @param userId The user ID
     * @return ApBalanceResponse containing AP balance information
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getApBalanceFallback")
    @Retry(name = "grpc")
    public ApBalanceResponse getApBalance(Long userId) {
        log.debug("gRPC: Calling Mining Service GetApBalance for userId: {}", userId);
        try {
            GetApBalanceRequest request = GetApBalanceRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return miningServiceStub.getApBalance(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting AP balance for user: {}", userId, e);
            throw new RuntimeException("Failed to get AP balance: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private ApBalanceResponse getApBalanceFallback(Long userId, Exception e) {
        log.warn("Circuit breaker open, returning zero AP balance for user: {}", userId);
        return ApBalanceResponse.newBuilder()
                .setUserId(userId != null ? userId : 0L)
                .setCurrentBalance("0")
                .setMonthlyAccumulated("0")
                .setLifetimeAccumulated("0")
                .setMiningActive(false)
                .build();
    }

    /**
     * Gets the mining speed for a user.
     *
     * @param userId The user ID
     * @return GetMiningSpeedResponse containing mining speed breakdown
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getMiningSpeedFallback")
    @Retry(name = "grpc")
    public GetMiningSpeedResponse getMiningSpeed(Long userId) {
        log.debug("gRPC: Calling Mining Service GetMiningSpeed for userId: {}", userId);
        try {
            GetMiningSpeedRequest request = GetMiningSpeedRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return miningServiceStub.getMiningSpeed(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting mining speed for user: {}", userId, e);
            throw new RuntimeException("Failed to get mining speed: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private GetMiningSpeedResponse getMiningSpeedFallback(Long userId, Exception e) {
        log.warn("Circuit breaker open, returning zero mining speed for user: {}", userId);
        return GetMiningSpeedResponse.newBuilder()
                .setTotalSpeed(0.0)
                .setBaseSpeed(0.0)
                .setFriendBonus(0.0)
                .setBookingBonus(0.0)
                .setVideoBoost(0.0)
                .setStatusBonus(0.0)
                .setIsCapped(false)
                .build();
    }

    /**
     * Resets monthly accumulated AP for a user.
     *
     * @param userId The user ID
     * @return ResetMonthlyAccumulatedResponse
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "resetMonthlyAccumulatedFallback")
    @Retry(name = "grpc")
    public ResetMonthlyAccumulatedResponse resetMonthlyAccumulated(Long userId) {
        log.debug("gRPC: Calling Mining Service ResetMonthlyAccumulated for userId: {}", userId);
        try {
            ResetMonthlyAccumulatedRequest request = ResetMonthlyAccumulatedRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return miningServiceStub.resetMonthlyAccumulated(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error resetting monthly accumulated for user: {}", userId, e);
            throw new RuntimeException("Failed to reset monthly accumulated: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private ResetMonthlyAccumulatedResponse resetMonthlyAccumulatedFallback(Long userId, Exception e) {
        log.warn("Circuit breaker open, returning failure for reset monthly accumulated: {}", userId);
        return ResetMonthlyAccumulatedResponse.newBuilder()
                .setSuccess(false)
                .setMessage("Service unavailable")
                .build();
    }

    /**
     * Credits AP to a user's balance.
     *
     * @param userId The user ID
     * @param amount The amount to credit
     * @param eventType The event type (e.g., "BONUS", "ADMIN_CREDIT")
     * @return CreditApResponse
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "creditApFallback")
    @Retry(name = "grpc")
    public CreditApResponse creditAp(Long userId, String amount, String eventType) {
        log.debug("gRPC: Calling Mining Service CreditAp for userId: {}, amount: {}", userId, amount);
        try {
            CreditApRequest request = CreditApRequest.newBuilder()
                    .setUserId(userId)
                    .setAmount(amount)
                    .setEventType(eventType)
                    .setReferenceId("")
                    .build();
            return miningServiceStub.creditAp(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error crediting AP for user: {}", userId, e);
            throw new RuntimeException("Failed to credit AP: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private CreditApResponse creditApFallback(Long userId, String amount, String eventType, Exception e) {
        log.warn("Circuit breaker open, returning failure for credit AP: {}", userId);
        return CreditApResponse.newBuilder()
                .setSuccess(false)
                .setNewBalance("0")
                .setMessage("Service unavailable")
                .build();
    }

    /**
     * Gets the count of users with active mining sessions.
     * 
     * <p>This counts users where miningActive = true in ApBalance,
     * which indicates they have an active mining session.
     *
     * @return GetActiveAutoMiningsCountResponse containing the count
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getActiveAutoMiningsCountFallback")
    @Retry(name = "grpc")
    public GetActiveAutoMiningsCountResponse getActiveAutoMiningsCount() {
        log.debug("gRPC: Calling Mining Service GetActiveAutoMiningsCount");
        try {
            GetActiveAutoMiningsCountRequest request = GetActiveAutoMiningsCountRequest.newBuilder().build();
            return miningServiceStub.getActiveAutoMiningsCount(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting active auto-minings count", e);
            throw new RuntimeException("Failed to get active auto-minings count: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private GetActiveAutoMiningsCountResponse getActiveAutoMiningsCountFallback(Exception e) {
        log.warn("Circuit breaker open, returning zero count for active auto-minings");
        return GetActiveAutoMiningsCountResponse.newBuilder()
                .setCount(0L)
                .build();
    }

    /**
     * Gets video boost history for a user with pagination.
     *
     * @param userId The user ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return GetVideoBoostHistoryResponse containing paginated video boost history
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getVideoBoostHistoryFallback")
    @Retry(name = "grpc")
    public GetVideoBoostHistoryResponse getVideoBoostHistory(Long userId, int page, int size) {
        log.debug("gRPC: Calling Mining Service GetVideoBoostHistory for userId: {}, page: {}, size: {}", userId, page, size);
        try {
            GetVideoBoostHistoryRequest request = GetVideoBoostHistoryRequest.newBuilder()
                    .setUserId(userId)
                    .setPage(page)
                    .setSize(size)
                    .build();
            return miningServiceStub.getVideoBoostHistory(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting video boost history for user: {}", userId, e);
            throw new RuntimeException("Failed to get video boost history: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private GetVideoBoostHistoryResponse getVideoBoostHistoryFallback(Long userId, int page, int size, Exception e) {
        log.warn("Circuit breaker open, returning empty video boost history for user: {}", userId);
        return GetVideoBoostHistoryResponse.newBuilder()
                .setPage(page)
                .setSize(size)
                .setTotalElements(0L)
                .setTotalPages(0)
                .build();
    }
}

