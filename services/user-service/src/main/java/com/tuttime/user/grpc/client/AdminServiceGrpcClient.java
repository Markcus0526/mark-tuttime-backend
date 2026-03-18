package com.tuttime.user.grpc.client;

import com.tuttime.common.proto.AdminServiceGrpc;
import com.tuttime.common.proto.GetAppVersionRequest;
import com.tuttime.common.proto.GetAppVersionResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC Client for Admin Service
 *
 * <p>This client provides methods to interact with Admin Service via gRPC.
 * Includes circuit breaker and retry mechanisms for resilience.
 */
@Slf4j
@Component
public class AdminServiceGrpcClient {

    @GrpcClient("admin-service")
    private AdminServiceGrpc.AdminServiceBlockingStub adminServiceStub;

    /**
     * Gets the current app version information.
     *
     * @return GetAppVersionResponse containing Android and iOS version info
     */
    @CircuitBreaker(name = "grpc", fallbackMethod = "getAppVersionFallback")
    @Retry(name = "grpc")
    public GetAppVersionResponse getAppVersion() {
        log.debug("gRPC: Calling Admin Service GetAppVersion");
        try {
            GetAppVersionRequest request = GetAppVersionRequest.newBuilder().build();
            return adminServiceStub.getAppVersion(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting app version", e);
            throw new RuntimeException("Failed to get app version: " + e.getMessage(), e);
        }
    }

    /**
     * Fallback method when circuit breaker is open
     */
    private GetAppVersionResponse getAppVersionFallback(Exception e) {
        log.warn("Circuit breaker open, returning default app version");
        return GetAppVersionResponse.newBuilder()
                .setAndroidVersion("1.0.0")
                .setAndroidInstallLink("https://play.google.com/store/apps/details?id=com.boostx.app")
                .setIosVersion("1.0.0")
                .setIosInstallLink("https://apps.apple.com/app/id123456789")
                .build();
    }
}

