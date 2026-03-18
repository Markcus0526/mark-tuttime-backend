package com.tuttime.user.grpc.client;

import com.tuttime.common.proto.ConversionServiceGrpc;
import com.tuttime.common.proto.GetConversionInfoRequest;
import com.tuttime.common.proto.GetConversionInfoResponse;
import com.tuttime.common.proto.GetUserConversionsRequest;
import com.tuttime.common.proto.GetUserConversionsResponse;
import com.tuttime.user.dto.ConversionInfo;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * gRPC Client for Conversion Service
 *
 * <p>This client provides methods to interact with Conversion Service via gRPC.
 * Includes circuit breaker and retry mechanisms for resilience.
 */
@Slf4j
@Component
public class ConversionServiceGrpcClient {

    @GrpcClient("conversion-service")
    private ConversionServiceGrpc.ConversionServiceBlockingStub conversionServiceStub;


    /**
     * Gets conversion information for a user.
     *
     * <p>This method calls Conversion Service via gRPC to get:
     * <ul>
     *   <li>Last month APART conversion amount</li>
     *   <li>Next conversion time (first day of next month at 00:01)</li>
     *   <li>This month forecast amount (estimated tokens)</li>
     * </ul>
     *
     * @param userId The user ID
     * @return ConversionInfo containing conversion-related data
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getConversionInfoFallback")
    @Retry(name = "grpc")
    public ConversionInfo getConversionInfo(Long userId) {
        log.info("gRPC: Calling Conversion Service GetConversionInfo for userId: {}", userId);
        try {
            if (userId == null) {
                throw new IllegalArgumentException("User ID cannot be null");
            }

            GetConversionInfoRequest request = GetConversionInfoRequest.newBuilder()
                    .setUserId(userId)
                    .build();

            log.debug("gRPC: Sending GetConversionInfo request to conversion-service for userId: {}", userId);
            GetConversionInfoResponse response = conversionServiceStub.getConversionInfo(request);

            // Parse response
            BigDecimal lastMonthConversion = new BigDecimal(response.getLastMonthApartConversion());
            OffsetDateTime nextConversionTime = OffsetDateTime.parse(response.getNextConversionTime()).withOffsetSameInstant(ZoneOffset.UTC);
            BigDecimal thisMonthForecast = new BigDecimal(response.getThisMonthForecastAmount());

            log.info("gRPC: Successfully received GetConversionInfo response from conversion-service for userId: {} - lastMonthConversion: {}, nextConversionTime: {}, thisMonthForecast: {}",
                    userId, lastMonthConversion, nextConversionTime, thisMonthForecast);

            return ConversionInfo.builder()
                    .lastMonthApartConversion(lastMonthConversion)
                    .nextConversionTime(nextConversionTime)
                    .thisMonthForecastAmount(thisMonthForecast)
                    .build();

        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting conversion info for user: {}", userId, e);
            throw new RuntimeException("Failed to get conversion info: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private ConversionInfo getConversionInfoFallback(Long userId, Exception e) {
        log.warn("Circuit breaker open, returning empty conversion info for user: {}", userId);
        return ConversionInfo.empty();
    }

    /**
     * Gets conversion history for a user.
     *
     * @param userId The user ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @return GetUserConversionsResponse containing conversion history
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getUserConversionsFallback")
    @Retry(name = "grpc")
    public GetUserConversionsResponse getUserConversions(Long userId, int page, int size) {
        log.debug("gRPC: Calling Conversion Service GetUserConversions for userId: {}, page: {}, size: {}", userId, page, size);
        try {
            GetUserConversionsRequest request = GetUserConversionsRequest.newBuilder()
                    .setUserId(userId)
                    .setPage(page)
                    .setSize(size)
                    .build();
            return conversionServiceStub.getUserConversions(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting user conversions for user: {}", userId, e);
            throw new RuntimeException("Failed to get user conversions: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private GetUserConversionsResponse getUserConversionsFallback(Long userId, int page, int size, Exception e) {
        log.warn("Circuit breaker open, returning empty conversions for user: {}", userId);
        return GetUserConversionsResponse.newBuilder()
                .setTotalPages(0)
                .setTotalElements(0)
                .setHasNext(false)
                .build();
    }
}

