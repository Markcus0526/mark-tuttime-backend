package com.tuttime.admin.grpc.client;

import com.tuttime.common.proto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * gRPC Client for Mining Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiningServiceGrpcClient {

	@GrpcClient("mining-service")
	private MiningServiceGrpc.MiningServiceBlockingStub miningServiceStub;

	@CircuitBreaker(name = "grpc", fallbackMethod = "getApBalanceFallback")
	@Retry(name = "grpc")
	public ApBalanceResponse getApBalance(Long userId) {
		try {
			GetApBalanceRequest request = GetApBalanceRequest.newBuilder()
					.setUserId(userId)
					.build();
			return miningServiceStub.getApBalance(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting AP balance: userId={}", userId, e);
			throw new RuntimeException("Failed to get AP balance: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "creditApFallback")
	@Retry(name = "grpc")
	public CreditApResponse creditAp(Long userId, BigDecimal amount, String eventType, String referenceId) {
		try {
			CreditApRequest request = CreditApRequest.newBuilder()
					.setUserId(userId)
					.setAmount(amount.toString())
					.setEventType(eventType)
					.setReferenceId(referenceId)
					.build();
			return miningServiceStub.creditAp(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error crediting AP: userId={}, amount={}", userId, amount, e);
			throw new RuntimeException("Failed to credit AP: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getMiningSpeedFallback")
	@Retry(name = "grpc")
	public GetMiningSpeedResponse getMiningSpeed(Long userId) {
		try {
			GetMiningSpeedRequest request = GetMiningSpeedRequest.newBuilder()
					.setUserId(userId)
					.build();
			return miningServiceStub.getMiningSpeed(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting mining speed: userId={}", userId, e);
			throw new RuntimeException("Failed to get mining speed: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getTotalMonthlyAccumulatedFallback")
	@Retry(name = "grpc")
	public GetTotalMonthlyAccumulatedResponse getTotalMonthlyAccumulated() {
		try {
			GetTotalMonthlyAccumulatedRequest request = GetTotalMonthlyAccumulatedRequest.newBuilder().build();
			return miningServiceStub.getTotalMonthlyAccumulated(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting total monthly accumulated", e);
			throw new RuntimeException("Failed to get total monthly accumulated: " + e.getMessage(), e);
		}
	}

	// Fallback methods
	private ApBalanceResponse getApBalanceFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning zero balance for user: {}", userId);
		return ApBalanceResponse.newBuilder()
				.setUserId(userId)
				.setCurrentBalance("0")
				.setMonthlyAccumulated("0")
				.setLifetimeAccumulated("0")
				.setMiningActive(false)
				.build();
	}

	private CreditApResponse creditApFallback(Long userId, BigDecimal amount, String eventType, String referenceId, Exception e) {
		log.warn("Circuit breaker open, credit AP failed for user: {}", userId);
		return CreditApResponse.newBuilder()
				.setSuccess(false)
				.setMessage("Service unavailable")
				.build();
	}

	private GetMiningSpeedResponse getMiningSpeedFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning default speed for user: {}", userId);
		return GetMiningSpeedResponse.newBuilder()
				.setTotalSpeed(1.0)
				.setBaseSpeed(1.0)
				.setFriendBonus(0.0)
				.setBookingBonus(0.0)
				.setVideoBoost(0.0)
				.setStatusBonus(0.0)
				.setIsCapped(false)
				.build();
	}

	private GetTotalMonthlyAccumulatedResponse getTotalMonthlyAccumulatedFallback(Exception e) {
		log.warn("Circuit breaker open, returning zero total");
		return GetTotalMonthlyAccumulatedResponse.newBuilder()
				.setTotalMonthlyAccumulated("0")
				.build();
	}

	/**
	 * Get count of users with active auto-mining enabled
	 * <p>
	 * NOTE: This endpoint needs to be added to mining-service proto:
	 * rpc GetActiveAutoMiningsCount (GetActiveAutoMiningsCountRequest) returns (GetActiveAutoMiningsCountResponse);
	 * <p>
	 * TODO: Add GetActiveAutoMiningsCount endpoint to mining_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getActiveAutoMiningsCountFallback")
	@Retry(name = "grpc")
	public GetActiveAutoMiningsCountResponse getActiveAutoMiningsCount() {
		try {
			GetActiveAutoMiningsCountRequest request = GetActiveAutoMiningsCountRequest.newBuilder().build();
			return miningServiceStub.getActiveAutoMiningsCount(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getActiveAutoMiningsCount not yet implemented in mining-service, using fallback");
				return getActiveAutoMiningsCountFallback(e);
			}
			log.error("gRPC error getting active auto-minings count", e);
			throw new RuntimeException("Failed to get active auto-minings count: " + e.getMessage(), e);
		}
	}

	/**
	 * Get active mining sessions count
	 * <p>
	 * NOTE: This endpoint needs to be added to mining-service proto:
	 * rpc GetActiveMiningSessionsCount (GetActiveMiningSessionsCountRequest) returns (GetActiveMiningSessionsCountResponse);
	 * <p>
	 * TODO: Add GetActiveMiningSessionsCount endpoint to mining_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getActiveMiningSessionsCountFallback")
	@Retry(name = "grpc")
	public GetActiveMiningSessionsCountResponse getActiveMiningSessionsCount() {
		try {
			GetActiveMiningSessionsCountRequest request = GetActiveMiningSessionsCountRequest.newBuilder().build();
			return miningServiceStub.getActiveMiningSessionsCount(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getActiveMiningSessionsCount not yet implemented in mining-service, using fallback");
				return getActiveMiningSessionsCountFallback(e);
			}
			log.error("gRPC error getting active mining sessions count", e);
			throw new RuntimeException("Failed to get active mining sessions count: " + e.getMessage(), e);
		}
	}

	/**
	 * Get AP growth history for a time period
	 * <p>
	 * NOTE: This endpoint needs to be added to mining-service proto:
	 * rpc GetApGrowthHistory (GetApGrowthHistoryRequest) returns (GetApGrowthHistoryResponse);
	 * <p>
	 * TODO: Add GetApGrowthHistory endpoint to mining_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getApGrowthHistoryFallback")
	@Retry(name = "grpc")
	public GetApGrowthHistoryResponse getApGrowthHistory(String startTime, String endTime) {
		try {
			GetApGrowthHistoryRequest request = GetApGrowthHistoryRequest.newBuilder()
					.setStartTime(startTime)
					.setEndTime(endTime)
					.build();
			return miningServiceStub.getApGrowthHistory(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getApGrowthHistory not yet implemented in mining-service, using fallback");
				return getApGrowthHistoryFallback(startTime, endTime, e);
			}
			log.error("gRPC error getting AP growth history", e);
			throw new RuntimeException("Failed to get AP growth history: " + e.getMessage(), e);
		}
	}

	// Fallback methods for new endpoints
	private GetActiveAutoMiningsCountResponse getActiveAutoMiningsCountFallback(Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning zero count");
		return GetActiveAutoMiningsCountResponse.newBuilder()
				.setCount(0L)
				.build();
	}

	private GetActiveMiningSessionsCountResponse getActiveMiningSessionsCountFallback(Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning zero count");
		return GetActiveMiningSessionsCountResponse.newBuilder()
				.setCount(0L)
				.build();
	}

	private GetApGrowthHistoryResponse getApGrowthHistoryFallback(String startTime, String endTime, Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty history");
		return GetApGrowthHistoryResponse.newBuilder().build();
	}

	/**
	 * Get top miners by AP Credits balance
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getTopMinersByApCreditsFallback")
	@Retry(name = "grpc")
	public GetTopMinersByApCreditsResponse getTopMinersByApCredits(Integer limit, String period) {
		try {
			GetTopMinersByApCreditsRequest request = GetTopMinersByApCreditsRequest.newBuilder()
					.setLimit(limit != null ? limit : 100)
					.setPeriod(period != null ? period : "TOTAL")
					.build();
			return miningServiceStub.getTopMinersByApCredits(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getTopMinersByApCredits not yet implemented in mining-service, using fallback");
				return getTopMinersByApCreditsFallback(limit, period, e);
			}
			log.error("gRPC error getting top miners by AP Credits", e);
			throw new RuntimeException("Failed to get top miners by AP Credits: " + e.getMessage(), e);
		}
	}

	// Fallback method for leaderboard endpoint
	private GetTopMinersByApCreditsResponse getTopMinersByApCreditsFallback(Integer limit, String period, Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty list");
		return GetTopMinersByApCreditsResponse.newBuilder().build();
	}
}

