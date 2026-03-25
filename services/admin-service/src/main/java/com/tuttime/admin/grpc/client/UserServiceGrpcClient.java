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
 * gRPC Client for User Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceGrpcClient {

	@GrpcClient("user-service")
	private UserServiceGrpc.UserServiceBlockingStub userServiceStub;

	@CircuitBreaker(name = "grpc", fallbackMethod = "getUserByIdFallback")
	@Retry(name = "grpc")
	public UserResponse getUserById(Long userId) {
		try {
			GetUserRequest request = GetUserRequest.newBuilder()
					.setUserId(userId)
					.build();
			return userServiceStub.getUserById(request);
		} catch (StatusRuntimeException e) {
			// Log NOT_FOUND as WARN (expected when user doesn't exist), other errors as ERROR
			if (e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND) {
				log.debug("User not found via gRPC: userId={}", userId);
			} else {
				log.error("gRPC error getting user: userId={}, status={}", userId, e.getStatus().getCode(), e);
			}
			throw new RuntimeException("Failed to get user: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "validateUserFallback")
	@Retry(name = "grpc")
	public ValidateUserResponse validateUser(Long userId) {
		try {
			ValidateUserRequest request = ValidateUserRequest.newBuilder()
					.setUserId(userId)
					.build();
			return userServiceStub.validateUser(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error validating user: userId={}", userId, e);
			throw new RuntimeException("Failed to validate user: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getUserStatusFallback")
	@Retry(name = "grpc")
	public UserStatusResponse getUserStatus(Long userId) {
		try {
			GetUserRequest request = GetUserRequest.newBuilder()
					.setUserId(userId)
					.build();
			return userServiceStub.getUserStatus(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting user status: userId={}", userId, e);
			throw new RuntimeException("Failed to get user status: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getKycStatusFallback")
	@Retry(name = "grpc")
	public KycStatusResponse getKycStatus(Long userId) {
		try {
			GetUserRequest request = GetUserRequest.newBuilder()
					.setUserId(userId)
					.build();
			return userServiceStub.isKycVerified(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting KYC status: userId={}", userId, e);
			throw new RuntimeException("Failed to get KYC status: " + e.getMessage(), e);
		}
	}

	// Fallback methods
	private UserResponse getUserByIdFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning null for user: {}", userId);
		return null;
	}

	private ValidateUserResponse validateUserFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning invalid user: {}", userId);
		return ValidateUserResponse.newBuilder()
				.setValid(false)
				.setActive(false)
				.setBlocked(true)
				.build();
	}

	private UserStatusResponse getUserStatusFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning default status for user: {}", userId);
		return UserStatusResponse.newBuilder()
				.setUserId(userId)
				.setIsActive(false)
				.setIsVerified(false)
				.setKycCompleted(false)
				.setIsBlocked(false)
				.setStatusLevel("BRONZE")
				.build();
	}

	private KycStatusResponse getKycStatusFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning default KYC status for user: {}", userId);
		return KycStatusResponse.newBuilder()
				.setUserId(userId)
				.setKycCompleted(false)
				.setKycVerified(false)
				.setKycStatus("PENDING")
				.build();
	}

	/**
	 * Get user statistics (total, active, blocked users)
	 * <p>
	 * NOTE: This endpoint needs to be added to user-service proto:
	 * rpc GetUserStatistics (GetUserStatisticsRequest) returns (GetUserStatisticsResponse);
	 * <p>
	 * TODO: Add GetUserStatistics endpoint to user_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getUserStatisticsFallback")
	@Retry(name = "grpc")
	public GetUserStatisticsResponse getUserStatistics() {
		try {
			// This will fail until the endpoint is added to user-service
			// For now, we'll catch the exception and use fallback
			GetUserStatisticsRequest request = GetUserStatisticsRequest.newBuilder().build();
			return userServiceStub.getUserStatistics(request);
		} catch (StatusRuntimeException e) {
			io.grpc.Status.Code statusCode = e.getStatus().getCode();
			if (statusCode == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getUserStatistics not yet implemented in user-service, using fallback");
				return getUserStatisticsFallback(e);
			}
			if (statusCode == io.grpc.Status.Code.UNAVAILABLE) {
				log.warn("User service unavailable, using fallback");
				return getUserStatisticsFallback(e);
			}
			log.error("gRPC error getting user statistics", e);
			throw new RuntimeException("Failed to get user statistics: " + e.getMessage(), e);
		}
	}

	/**
	 * Get users count by level (BRONZE, SILVER, GOLD, PLATINUM, DIAMOND, ELITE)
	 * <p>
	 * NOTE: This endpoint needs to be added to user-service proto:
	 * rpc GetUsersByLevel (GetUsersByLevelRequest) returns (GetUsersByLevelResponse);
	 * <p>
	 * TODO: Add GetUsersByLevel endpoint to user_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getUsersByLevelFallback")
	@Retry(name = "grpc")
	public GetUsersByLevelResponse getUsersByLevel() {
		try {
			// This will fail until the endpoint is added to user-service
			GetUsersByLevelRequest request = GetUsersByLevelRequest.newBuilder().build();
			return userServiceStub.getUsersByLevel(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getUsersByLevel not yet implemented in user-service, using fallback");
				return getUsersByLevelFallback(e);
			}
			log.error("gRPC error getting users by level", e);
			throw new RuntimeException("Failed to get users by level: " + e.getMessage(), e);
		}
	}

	// Fallback methods for new endpoints
	private GetUserStatisticsResponse getUserStatisticsFallback(Exception e) {
		log.error("Circuit breaker open or endpoint not implemented, returning default statistics. Error: {}",
				e != null ? e.getMessage() : "Unknown error", e);
		// Return zeros instead of throwing - this allows dashboard to render
		// but logs the error so we know there's a problem
		return GetUserStatisticsResponse.newBuilder()
				.setTotalUsers(0L)
				.setActiveUsers(0L)
				.setBlockedUsers(0L)
				.build();
	}

	private GetUsersByLevelResponse getUsersByLevelFallback(Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty level map");
		return GetUsersByLevelResponse.newBuilder()
				.putUsersByLevel("BRONZE", 0L)
				.putUsersByLevel("SILVER", 0L)
				.putUsersByLevel("GOLD", 0L)
				.putUsersByLevel("PLATINUM", 0L)
				.putUsersByLevel("DIAMOND", 0L)
				.putUsersByLevel("ELITE", 0L)
				.build();
	}

	/**
	 * Get top users by AP Credits balance
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getTopUsersByApCreditsFallback")
	@Retry(name = "grpc")
	public GetTopUsersByApCreditsResponse getTopUsersByApCredits(Integer limit, String period) {
		try {
			GetTopUsersByApCreditsRequest request = GetTopUsersByApCreditsRequest.newBuilder()
					.setLimit(limit != null ? limit : 100)
					.setPeriod(period != null ? period : "TOTAL")
					.build();
			return userServiceStub.getTopUsersByApCredits(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getTopUsersByApCredits not yet implemented in user-service, using fallback");
				return getTopUsersByApCreditsFallback(limit, period, e);
			}
			log.error("gRPC error getting top users by AP Credits", e);
			throw new RuntimeException("Failed to get top users by AP Credits: " + e.getMessage(), e);
		}
	}

	/**
	 * Get top users by APART balance
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getTopUsersByApartBalanceFallback")
	@Retry(name = "grpc")
	public GetTopUsersByApartBalanceResponse getTopUsersByApartBalance(Integer limit, String period) {
		try {
			GetTopUsersByApartBalanceRequest request = GetTopUsersByApartBalanceRequest.newBuilder()
					.setLimit(limit != null ? limit : 100)
					.setPeriod(period != null ? period : "TOTAL")
					.build();
			return userServiceStub.getTopUsersByApartBalance(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getTopUsersByApartBalance not yet implemented in user-service, using fallback");
				return getTopUsersByApartBalanceFallback(limit, period, e);
			}
			log.error("gRPC error getting top users by APART balance", e);
			throw new RuntimeException("Failed to get top users by APART balance: " + e.getMessage(), e);
		}
	}

	/**
	 * Get geolocation statistics
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getGeolocationStatsFallback")
	@Retry(name = "grpc")
	public GetGeolocationStatsResponse getGeolocationStats(String country, String city) {
		try {
			GetGeolocationStatsRequest.Builder requestBuilder = GetGeolocationStatsRequest.newBuilder();
			if (country != null && !country.isEmpty()) {
				requestBuilder.setCountry(country);
			}
			if (city != null && !city.isEmpty()) {
				requestBuilder.setCity(city);
			}
			return userServiceStub.getGeolocationStats(requestBuilder.build());
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getGeolocationStats not yet implemented in user-service, using fallback");
				return getGeolocationStatsFallback(country, city, e);
			}
			log.error("gRPC error getting geolocation stats", e);
			throw new RuntimeException("Failed to get geolocation stats: " + e.getMessage(), e);
		}
	}

	// Fallback methods for leaderboard endpoints
	private GetTopUsersByApCreditsResponse getTopUsersByApCreditsFallback(Integer limit, String period, Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty list");
		return GetTopUsersByApCreditsResponse.newBuilder().build();
	}

	private GetTopUsersByApartBalanceResponse getTopUsersByApartBalanceFallback(Integer limit, String period, Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty list");
		return GetTopUsersByApartBalanceResponse.newBuilder().build();
	}

	private GetGeolocationStatsResponse getGeolocationStatsFallback(String country, String city, Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty list");
		return GetGeolocationStatsResponse.newBuilder().build();
	}

	/**
	 * Get registrations per day
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getRegistrationsPerDayFallback")
	@Retry(name = "grpc")
	public GetRegistrationsPerDayResponse getRegistrationsPerDay(java.time.LocalDate startDate, java.time.LocalDate endDate) {
		try {
			GetRegistrationsPerDayRequest request = GetRegistrationsPerDayRequest.newBuilder()
					.setStartDate(startDate != null ? startDate.toString() : "")
					.setEndDate(endDate != null ? endDate.toString() : "")
					.build();
			return userServiceStub.getRegistrationsPerDay(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getRegistrationsPerDay not yet implemented in user-service, using fallback");
				return getRegistrationsPerDayFallback(startDate, endDate, e);
			}
			log.error("gRPC error getting registrations per day", e);
			throw new RuntimeException("Failed to get registrations per day: " + e.getMessage(), e);
		}
	}

	/**
	 * Get registrations per month
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getRegistrationsPerMonthFallback")
	@Retry(name = "grpc")
	public GetRegistrationsPerMonthResponse getRegistrationsPerMonth(java.time.LocalDate startDate, java.time.LocalDate endDate) {
		try {
			GetRegistrationsPerMonthRequest request = GetRegistrationsPerMonthRequest.newBuilder()
					.setStartDate(startDate != null ? startDate.toString() : "")
					.setEndDate(endDate != null ? endDate.toString() : "")
					.build();
			return userServiceStub.getRegistrationsPerMonth(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getRegistrationsPerMonth not yet implemented in user-service, using fallback");
				return getRegistrationsPerMonthFallback(startDate, endDate, e);
			}
			log.error("gRPC error getting registrations per month", e);
			throw new RuntimeException("Failed to get registrations per month: " + e.getMessage(), e);
		}
	}

	// Fallback methods for registration statistics
	private GetRegistrationsPerDayResponse getRegistrationsPerDayFallback(java.time.LocalDate startDate, java.time.LocalDate endDate, Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty list");
		return GetRegistrationsPerDayResponse.newBuilder().build();
	}

	private GetRegistrationsPerMonthResponse getRegistrationsPerMonthFallback(java.time.LocalDate startDate, java.time.LocalDate endDate, Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty list");
		return GetRegistrationsPerMonthResponse.newBuilder().build();
	}
}

