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
 * gRPC Client for Token Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenServiceGrpcClient {

	@GrpcClient("token-service")
	private TokenServiceGrpc.TokenServiceBlockingStub tokenServiceStub;

	@CircuitBreaker(name = "grpc", fallbackMethod = "getBalanceFallback")
	@Retry(name = "grpc")
	public BalanceResponse getBalance(Long userId) {
		try {
			GetBalanceRequest request = GetBalanceRequest.newBuilder()
					.setUserId(userId)
					.build();
			BalanceResponse response = tokenServiceStub.getBalance(request);

			// Validate response and handle null balances
			if (response == null) {
				log.warn("Token service returned null balance response for userId={}, using fallback", userId);
				return getBalanceFallback(userId, new RuntimeException("Null response from token service"));
			}

			// Check if balance fields are null or empty, return safe fallback if so
			if ((response.getBalance() == null || response.getBalance().isEmpty()) &&
					(response.getAvailableBalance() == null || response.getAvailableBalance().isEmpty())) {
				log.warn("Token service returned null/empty balance for userId={}, using fallback", userId);
				return getBalanceFallback(userId, new RuntimeException("Null balance in response"));
			}

			return response;
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting balance: userId={}", userId, e);
			// If it's an internal error related to null values, use fallback
			if (e.getStatus().getCode() == io.grpc.Status.Code.INTERNAL &&
					e.getMessage() != null && e.getMessage().contains("null")) {
				log.warn("Token service internal error with null values for userId={}, using fallback", userId);
				return getBalanceFallback(userId, e);
			}
			throw new RuntimeException("Failed to get balance: " + e.getMessage(), e);
		} catch (Exception e) {
			log.error("Unexpected error getting balance: userId={}", userId, e);
			return getBalanceFallback(userId, e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "transferTokensFallback")
	@Retry(name = "grpc")
	public TransferResponse transferTokens(Long fromUserId, Long toUserId, BigDecimal amount,
	                                       String type, String referenceType, String referenceId) {
		try {
			TransferRequest request = TransferRequest.newBuilder()
					.setFromUserId(fromUserId)
					.setToUserId(toUserId)
					.setAmount(amount.toString())
					.setType(type)
					.setReferenceType(referenceType)
					.setReferenceId(referenceId)
					.build();
			return tokenServiceStub.transferTokens(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error transferring tokens: from={}, to={}, amount={}", fromUserId, toUserId, amount, e);
			throw new RuntimeException("Failed to transfer tokens: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "createTransactionFallback")
	@Retry(name = "grpc")
	public TransactionResponse createTransaction(Long userId, String type, BigDecimal amount,
	                                             String status, Long fromUserId, Long toUserId,
	                                             String referenceType, String referenceId) {
		try {
			CreateTransactionRequest request = CreateTransactionRequest.newBuilder()
					.setUserId(userId)
					.setType(type)
					.setAmount(amount.toString())
					.setStatus(status)
					.setFromUserId(fromUserId != null ? fromUserId : 0)
					.setToUserId(toUserId != null ? toUserId : 0)
					.setReferenceType(referenceType)
					.setReferenceId(referenceId)
					.build();
			return tokenServiceStub.createTransaction(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error creating transaction: userId={}", userId, e);
			throw new RuntimeException("Failed to create transaction: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getUserStatusFallback")
	@Retry(name = "grpc")
	public GetUserStatusResponse getUserStatus(Long userId) {
		try {
			GetUserStatusRequest request = GetUserStatusRequest.newBuilder()
					.setUserId(userId)
					.build();
			return tokenServiceStub.getUserStatus(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting user status: userId={}", userId, e);
			throw new RuntimeException("Failed to get user status: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "burnTokensFallback")
	@Retry(name = "grpc")
	public BurnTokensResponse burnTokens(Long userId, BigDecimal amount, String reason, String referenceType, String referenceId) {
		try {
			BurnTokensRequest request = BurnTokensRequest.newBuilder()
					.setUserId(userId)
					.setAmount(amount.toPlainString())
					.setReason(reason != null ? reason : "ADMIN_BURN")
					.setReferenceType(referenceType)
					.setReferenceId(referenceId != null ? Long.parseLong(referenceId) : 0L)
					.build();
			return tokenServiceStub.burnTokens(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error burning tokens: userId={}, amount={}", userId, amount, e);
			throw new RuntimeException("Failed to burn tokens: " + e.getMessage(), e);
		}
	}

	// Fallback methods
	private BalanceResponse getBalanceFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning zero balance for user: {}", userId);
		return BalanceResponse.newBuilder()
				.setUserId(userId)
				.setBalance("0")
				.setLockedAmount("0")
				.setAvailableBalance("0")
				.build();
	}

	private TransferResponse transferTokensFallback(Long fromUserId, Long toUserId, BigDecimal amount,
	                                                String type, String referenceType, String referenceId, Exception e) {
		log.warn("Circuit breaker open, transfer failed");
		return TransferResponse.newBuilder()
				.setSuccess(false)
				.setMessage("Service unavailable")
				.build();
	}

	private TransactionResponse createTransactionFallback(Long userId, String type, BigDecimal amount,
	                                                      String status, Long fromUserId, Long toUserId,
	                                                      String referenceType, String referenceId, Exception e) {
		log.warn("Circuit breaker open, transaction creation failed");
		return TransactionResponse.newBuilder()
				.setTransactionId("")
				.build();
	}

	private GetUserStatusResponse getUserStatusFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning default status");
		return GetUserStatusResponse.newBuilder()
				.setStatusLevel("BRONZE")
				.setStatusBonusApPerHour("0")
				.build();
	}

	private BurnTokensResponse burnTokensFallback(Long userId, BigDecimal amount, String reason, String referenceType,
	                                              String referenceId, Exception e) {
		log.warn("Circuit breaker open, burn failed");
		return BurnTokensResponse.newBuilder()
				.setSuccess(false)
				.setMessage("Service unavailable")
				.build();
	}

	/**
	 * Get total APART tokens balance across all users
	 * <p>
	 * NOTE: This endpoint needs to be added to token-service proto:
	 * rpc GetTotalApartBalance (GetTotalApartBalanceRequest) returns (GetTotalApartBalanceResponse);
	 * <p>
	 * TODO: Add GetTotalApartBalance endpoint to token_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getTotalApartBalanceFallback")
	@Retry(name = "grpc")
	public GetTotalApartBalanceResponse getTotalApartBalance() {
		try {
			GetTotalApartBalanceRequest request = GetTotalApartBalanceRequest.newBuilder().build();
			return tokenServiceStub.getTotalApartBalance(request);
		} catch (StatusRuntimeException e) {
			if (e.getStatus().getCode() == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getTotalApartBalance not yet implemented in token-service, using fallback");
				return getTotalApartBalanceFallback(e);
			}
			log.error("gRPC error getting total APART balance", e);
			throw new RuntimeException("Failed to get total APART balance: " + e.getMessage(), e);
		}
	}

	/**
	 * Get APART growth history for a time period
	 * <p>
	 * NOTE: This endpoint needs to be added to token-service proto:
	 * rpc GetApartGrowthHistory (GetApartGrowthHistoryRequest) returns (GetApartGrowthHistoryResponse);
	 * <p>
	 * TODO: Add GetApartGrowthHistory endpoint to token_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getApartGrowthHistoryFallback")
	@Retry(name = "grpc")
	public GetApartGrowthHistoryResponse getApartGrowthHistory(String startTime, String endTime) {
		try {
			GetApartGrowthHistoryRequest request = GetApartGrowthHistoryRequest.newBuilder()
					.setStartTime(startTime)
					.setEndTime(endTime)
					.build();
			return tokenServiceStub.getApartGrowthHistory(request);
		} catch (StatusRuntimeException e) {
			io.grpc.Status.Code statusCode = e.getStatus().getCode();
			if (statusCode == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getApartGrowthHistory not yet implemented in token-service, using fallback");
				return getApartGrowthHistoryFallback(startTime, endTime, e);
			}
			if (statusCode == io.grpc.Status.Code.UNAVAILABLE) {
				log.warn("Token service unavailable, using fallback");
				return getApartGrowthHistoryFallback(startTime, endTime, e);
			}
			log.error("gRPC error getting APART growth history", e);
			throw new RuntimeException("Failed to get APART growth history: " + e.getMessage(), e);
		}
	}

	// Fallback methods for new endpoints
	private GetTotalApartBalanceResponse getTotalApartBalanceFallback(Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning zero balance");
		return GetTotalApartBalanceResponse.newBuilder()
				.setTotalBalance("0")
				.build();
	}

	private GetApartGrowthHistoryResponse getApartGrowthHistoryFallback(String startTime, String endTime, Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty history");
		return GetApartGrowthHistoryResponse.newBuilder().build();
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getDepositHistoryFallback")
	@Retry(name = "grpc")
	public GetDepositHistoryResponse getDepositHistory(String status, String network, String startDate, String endDate, int page, int size) {
		try {
			GetDepositHistoryRequest request = GetDepositHistoryRequest.newBuilder()
					.setStatus(status != null ? status : "")
					.setNetwork(network != null ? network : "")
					.setStartDate(startDate != null ? startDate : "")
					.setEndDate(endDate != null ? endDate : "")
					.setPage(page)
					.setSize(size)
					.build();
			return tokenServiceStub.getDepositHistory(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting deposit history", e);
			throw new RuntimeException("Failed to get deposit history: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getWithdrawalHistoryFallback")
	@Retry(name = "grpc")
	public GetWithdrawalHistoryResponse getWithdrawalHistory(String status, String network, String startDate, String endDate, int page, int size) {
		try {
			GetWithdrawalHistoryRequest request = GetWithdrawalHistoryRequest.newBuilder()
					.setStatus(status != null ? status : "")
					.setNetwork(network != null ? network : "")
					.setStartDate(startDate != null ? startDate : "")
					.setEndDate(endDate != null ? endDate : "")
					.setPage(page)
					.setSize(size)
					.build();
			return tokenServiceStub.getWithdrawalHistory(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting withdrawal history", e);
			throw new RuntimeException("Failed to get withdrawal history: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getBurnHistoryFallback")
	@Retry(name = "grpc")
	public GetBurnHistoryResponse getBurnHistory(String startDate, String endDate, int page, int size) {
		try {
			GetBurnHistoryRequest request = GetBurnHistoryRequest.newBuilder()
					.setStartDate(startDate != null ? startDate : "")
					.setEndDate(endDate != null ? endDate : "")
					.setPage(page)
					.setSize(size)
					.build();
			return tokenServiceStub.getBurnHistory(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting burn history", e);
			throw new RuntimeException("Failed to get burn history: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getClaimHistoryFallback")
	@Retry(name = "grpc")
	public GetClaimHistoryResponse getClaimHistory(Long userId, String startDate, String endDate, int page, int size) {
		try {
			GetClaimHistoryRequest request = GetClaimHistoryRequest.newBuilder()
					.setUserId(userId != null ? userId : 0)
					.setStartDate(startDate != null ? startDate : "")
					.setEndDate(endDate != null ? endDate : "")
					.setPage(page)
					.setSize(size)
					.build();
			return tokenServiceStub.getClaimHistory(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting claim history", e);
			throw new RuntimeException("Failed to get claim history: " + e.getMessage(), e);
		}
	}

	// Fallback methods
	private GetDepositHistoryResponse getDepositHistoryFallback(String status, String network, String startDate, String endDate, int page, int size, Exception e) {
		log.warn("Circuit breaker open, returning empty deposit history");
		return GetDepositHistoryResponse.newBuilder()
				.setTotalPages(0)
				.setTotalElements(0)
				.setHasNext(false)
				.build();
	}

	private GetWithdrawalHistoryResponse getWithdrawalHistoryFallback(String status, String network, String startDate, String endDate, int page, int size, Exception e) {
		log.warn("Circuit breaker open, returning empty withdrawal history");
		return GetWithdrawalHistoryResponse.newBuilder()
				.setTotalPages(0)
				.setTotalElements(0)
				.setHasNext(false)
				.build();
	}

	private GetBurnHistoryResponse getBurnHistoryFallback(String startDate, String endDate, int page, int size, Exception e) {
		log.warn("Circuit breaker open, returning empty burn history");
		return GetBurnHistoryResponse.newBuilder()
				.setTotalPages(0)
				.setTotalElements(0)
				.setHasNext(false)
				.build();
	}

	private GetClaimHistoryResponse getClaimHistoryFallback(Long userId, String startDate, String endDate, int page, int size, Exception e) {
		log.warn("Circuit breaker open, returning empty claim history");
		return GetClaimHistoryResponse.newBuilder()
				.setTotalPages(0)
				.setTotalElements(0)
				.setHasNext(false)
				.build();
	}
}

