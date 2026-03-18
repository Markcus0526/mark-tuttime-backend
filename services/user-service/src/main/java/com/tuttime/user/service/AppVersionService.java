package com.tuttime.user.service;

import com.tuttime.common.proto.GetAppVersionResponse;
import com.tuttime.user.dto.AppVersionResponse;
import com.tuttime.user.grpc.client.AdminServiceGrpcClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * App Version Service
 * 
 * <p>Service for retrieving app version information for authenticated users.
 * Uses gRPC to communicate with admin-service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppVersionService {

    private final AdminServiceGrpcClient adminServiceGrpcClient;

    /**
     * Get current app version information
     * 
     * <p>Retrieves app version from admin-service via gRPC. Results are cached for 5 minutes
     * to reduce load on admin-service.
     * 
     * @return AppVersionResponse containing version info for Android and iOS
     */
    @Cacheable(value = "appVersion", unless = "#result == null")
    public AppVersionResponse getAppVersion() {
        log.debug("Getting app version for user via gRPC");
        GetAppVersionResponse grpcResponse = adminServiceGrpcClient.getAppVersion();
        
        return AppVersionResponse.builder()
                .androidVersion(grpcResponse.getAndroidVersion())
                .androidInstallLink(grpcResponse.getAndroidInstallLink())
                .iosVersion(grpcResponse.getIosVersion())
                .iosInstallLink(grpcResponse.getIosInstallLink())
                .build();
    }
}

