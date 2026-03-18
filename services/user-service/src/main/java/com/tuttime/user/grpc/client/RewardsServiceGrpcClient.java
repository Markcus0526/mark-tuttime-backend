package com.tuttime.user.grpc.client;

import com.tuttime.common.proto.*;
import com.tuttime.common.proto.ReferralCodeResponse;
import com.tuttime.common.proto.GetReferrerByCodeResponse;
import com.tuttime.common.proto.ValidateReferralCodeResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC Client for Rewards Service
 *
 * <p>This client provides methods to interact with Rewards Service via gRPC.
 * Includes circuit breaker and retry mechanisms for resilience.
 */
@Slf4j
@Component
public class RewardsServiceGrpcClient {

    @GrpcClient("rewards-service")
    private RewardsServiceGrpc.RewardsServiceBlockingStub rewardsServiceStub;

    /**
     * Gets the referral code for a user.
     *
     * @param userId The user ID
     * @return ReferralCodeResponse containing referral code and level
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getReferralCodeFallback")
    @Retry(name = "grpc")
    public ReferralCodeResponse getReferralCode(Long userId) {
        log.debug("gRPC: Calling Rewards Service GetReferralCode for userId: {}", userId);
        try {
            GetReferralCodeRequest request = GetReferralCodeRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            return rewardsServiceStub.getReferralCode(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting referral code for user: {}", userId, e);
            throw new RuntimeException("Failed to get referral code: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private ReferralCodeResponse getReferralCodeFallback(Long userId, Exception e) {
        log.warn("Circuit breaker open, returning empty referral code for user: {}", userId);
        return ReferralCodeResponse.newBuilder()
                .setReferralCode("")
                .setReferralLevel(0)
                .build();
    }

    /**
     * Gets the referrer ID by referral code.
     *
     * @param referralCode The referral code
     * @return GetReferrerByCodeResponse containing referrer ID and level, or null if invalid
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getReferrerByCodeFallback")
    @Retry(name = "grpc")
    public GetReferrerByCodeResponse getReferrerByCode(String referralCode) {
        log.debug("gRPC: Calling Rewards Service GetReferrerByCode for referralCode: {}", referralCode);
        try {
            GetReferrerByCodeRequest request = GetReferrerByCodeRequest.newBuilder()
                    .setReferralCode(referralCode)
                    .build();
            return rewardsServiceStub.getReferrerByCode(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting referrer by code: {}", referralCode, e);
            throw new RuntimeException("Failed to get referrer by code: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private GetReferrerByCodeResponse getReferrerByCodeFallback(String referralCode, Exception e) {
        log.warn("Circuit breaker open, returning invalid response for referral code: {}", referralCode);
        return GetReferrerByCodeResponse.newBuilder()
                .setValid(false)
                .setReferrerId(0)
                .setReferralLevel(0)
                .build();
    }

    /**
     * Validates a referral code.
     *
     * @param referralCode The referral code to validate
     * @return ValidateReferralCodeResponse containing validation result and referrer info
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "validateReferralCodeFallback")
    @Retry(name = "grpc")
    public ValidateReferralCodeResponse validateReferralCode(String referralCode) {
        log.debug("gRPC: Calling Rewards Service ValidateReferralCode for referralCode: {}", referralCode);
        try {
            ValidateReferralCodeRequest request = ValidateReferralCodeRequest.newBuilder()
                    .setReferralCode(referralCode)
                    .build();
            return rewardsServiceStub.validateReferralCode(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error validating referral code: {}", referralCode, e);
            throw new RuntimeException("Failed to validate referral code: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private ValidateReferralCodeResponse validateReferralCodeFallback(String referralCode, Exception e) {
        log.warn("Circuit breaker open, returning invalid response for referral code: {}", referralCode);
        return ValidateReferralCodeResponse.newBuilder()
                .setValid(false)
                .setMessage("Referral service unavailable")
                .setReferrerId(0)
                .setReferrerLevel(0)
                .build();
    }
}

