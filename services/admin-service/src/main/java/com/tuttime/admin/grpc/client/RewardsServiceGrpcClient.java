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
 * gRPC Client for Rewards Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RewardsServiceGrpcClient {

	@GrpcClient("rewards-service")
	private RewardsServiceGrpc.RewardsServiceBlockingStub rewardsServiceStub;

	@CircuitBreaker(name = "grpc", fallbackMethod = "getReferralCodeFallback")
	@Retry(name = "grpc")
	public ReferralCodeResponse getReferralCode(Long userId) {
		try {
			GetReferralCodeRequest request = GetReferralCodeRequest.newBuilder()
					.setUserId(userId)
					.build();
			return rewardsServiceStub.getReferralCode(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting referral code: userId={}", userId, e);
			throw new RuntimeException("Failed to get referral code: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getActiveFriendsCountFallback")
	@Retry(name = "grpc")
	public ActiveFriendsCountResponse getActiveFriendsCount(Long userId) {
		try {
			GetActiveFriendsCountRequest request = GetActiveFriendsCountRequest.newBuilder()
					.setUserId(userId)
					.build();
			return rewardsServiceStub.getActiveFriendsCount(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting active friends count: userId={}", userId, e);
			throw new RuntimeException("Failed to get active friends count: " + e.getMessage(), e);
		}
	}

	// Fallback methods
	private ReferralCodeResponse getReferralCodeFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning empty referral code");
		return ReferralCodeResponse.newBuilder()
				.setReferralCode("")
				.setReferralLevel(0)
				.build();
	}

	private ActiveFriendsCountResponse getActiveFriendsCountFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning zero friends count");
		return ActiveFriendsCountResponse.newBuilder()
				.setUserId(userId)
				.setCount(0)
				.build();
	}

	/**
	 * Get total referrals count across all users
	 * <p>
	 * NOTE: This endpoint needs to be added to rewards-service proto:
	 * rpc GetTotalReferralsCount (GetTotalReferralsCountRequest) returns (GetTotalReferralsCountResponse);
	 * <p>
	 * TODO: Add GetTotalReferralsCount endpoint to rewards_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getTotalReferralsCountFallback")
	@Retry(name = "grpc")
	public GetTotalReferralsCountResponse getTotalReferralsCount() {
		try {
			GetTotalReferralsCountRequest request = GetTotalReferralsCountRequest.newBuilder().build();
			return rewardsServiceStub.getTotalReferralsCount(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getTotalReferralsCount not yet implemented in rewards-service, using fallback");
				return getTotalReferralsCountFallback(e);
			}
			log.error("gRPC error getting total referrals count", e);
			throw new RuntimeException("Failed to get total referrals count: " + e.getMessage(), e);
		}
	}

	// Fallback method for new endpoint
	private GetTotalReferralsCountResponse getTotalReferralsCountFallback(Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning zero count");
		return GetTotalReferralsCountResponse.newBuilder()
				.setTotalReferrals(0L)
				.build();
	}
}

