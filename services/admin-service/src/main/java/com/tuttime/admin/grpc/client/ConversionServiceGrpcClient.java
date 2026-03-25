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
 * gRPC Client for Conversion Service
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversionServiceGrpcClient {

	@GrpcClient("conversion-service")
	private ConversionServiceGrpc.ConversionServiceBlockingStub conversionServiceStub;

	@CircuitBreaker(name = "grpc", fallbackMethod = "getConversionInfoFallback")
	@Retry(name = "grpc")
	public GetConversionInfoResponse getConversionInfo(Long userId) {
		try {
			GetConversionInfoRequest request = GetConversionInfoRequest.newBuilder()
					.setUserId(userId)
					.build();
			return conversionServiceStub.getConversionInfo(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting conversion info: userId={}", userId, e);
			throw new RuntimeException("Failed to get conversion info: " + e.getMessage(), e);
		}
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getUserConversionsFallback")
	@Retry(name = "grpc")
	public GetUserConversionsResponse getUserConversions(Long userId, int page, int size) {
		try {
			GetUserConversionsRequest request = GetUserConversionsRequest.newBuilder()
					.setUserId(userId)
					.setPage(page)
					.setSize(size)
					.build();
			return conversionServiceStub.getUserConversions(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting user conversions: userId={}", userId, e);
			throw new RuntimeException("Failed to get user conversions: " + e.getMessage(), e);
		}
	}

	// Fallback methods
	private GetConversionInfoResponse getConversionInfoFallback(Long userId, Exception e) {
		log.warn("Circuit breaker open, returning default conversion info");
		return GetConversionInfoResponse.newBuilder()
				.setLastMonthApartConversion("0")
				.setNextConversionTime("")
				.setThisMonthForecastAmount("0")
				.build();
	}

	private GetUserConversionsResponse getUserConversionsFallback(Long userId, int page, int size, Exception e) {
		log.warn("Circuit breaker open, returning empty conversions");
		return GetUserConversionsResponse.newBuilder()
				.setTotalPages(0)
				.setTotalElements(0)
				.setHasNext(false)
				.build();
	}

	/**
	 * Get last conversion summary statistics
	 * <p>
	 * NOTE: This endpoint needs to be added to conversion-service proto:
	 * rpc GetLastConversionSummary (GetLastConversionSummaryRequest) returns (GetLastConversionSummaryResponse);
	 * <p>
	 * TODO: Add GetLastConversionSummary endpoint to conversion_service.proto
	 */
	@CircuitBreaker(name = "grpc", fallbackMethod = "getLastConversionSummaryFallback")
	@Retry(name = "grpc")
	public GetLastConversionSummaryResponse getLastConversionSummary() {
		try {
			GetLastConversionSummaryRequest request = GetLastConversionSummaryRequest.newBuilder().build();
			return conversionServiceStub.getLastConversionSummary(request);
		} catch (StatusRuntimeException e) {
			io.grpc.Status.Code statusCode = e.getStatus().getCode();
			if (statusCode == io.grpc.Status.Code.UNIMPLEMENTED) {
				log.debug("getLastConversionSummary not yet implemented in conversion-service, using fallback");
				return getLastConversionSummaryFallback(e);
			}
			if (statusCode == io.grpc.Status.Code.UNAVAILABLE) {
				log.warn("Conversion service unavailable, using fallback");
				return getLastConversionSummaryFallback(e);
			}
			log.error("gRPC error getting last conversion summary", e);
			throw new RuntimeException("Failed to get last conversion summary: " + e.getMessage(), e);
		}
	}

	// Fallback method for new endpoint
	private GetLastConversionSummaryResponse getLastConversionSummaryFallback(Exception e) {
		log.warn("Circuit breaker open or endpoint not implemented, returning empty summary");
		return GetLastConversionSummaryResponse.newBuilder()
				.setTotalUsersCount(0L)
				.setTotalApAmount("0")
				.setAverageUserShare("0")
				.setAverageDistributedApart("0")
				.setConversionDate("")
				.setConversionId(0L)
				.build();
	}

	@CircuitBreaker(name = "grpc", fallbackMethod = "getMonthlyConversionLedgerFallback")
	@Retry(name = "grpc")
	public GetMonthlyConversionLedgerResponse getMonthlyConversionLedger(int year, int month, int page, int size) {
		try {
			GetMonthlyConversionLedgerRequest request = GetMonthlyConversionLedgerRequest.newBuilder()
					.setYear(year)
					.setMonth(month)
					.setPage(page)
					.setSize(size)
					.build();
			return conversionServiceStub.getMonthlyConversionLedger(request);
		} catch (StatusRuntimeException e) {
			log.error("gRPC error getting monthly conversion ledger: year={}, month={}", year, month, e);
			throw new RuntimeException("Failed to get monthly conversion ledger: " + e.getMessage(), e);
		}
	}

	private GetMonthlyConversionLedgerResponse getMonthlyConversionLedgerFallback(int year, int month, int page, int size, Exception e) {
		log.warn("Circuit breaker open, returning empty ledger");
		return GetMonthlyConversionLedgerResponse.newBuilder()
				.setTotalPages(0)
				.setTotalElements(0)
				.setHasNext(false)
				.build();
	}
}

