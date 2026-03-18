package com.tuttime.admin.grpc;

import com.tuttime.admin.service.AdminMiningFormulaService;
import com.tuttime.admin.dto.MiningFormulaConfigResponse;
import com.tuttime.admin.service.AppVersionService;
import com.tuttime.admin.dto.AppVersionResponse;
import com.tuttime.common.proto.AdminServiceGrpc;
import com.tuttime.common.proto.GetMiningFormulaConfigRequest;
import com.tuttime.common.proto.GetMiningFormulaConfigResponse;
import com.tuttime.common.proto.GetAppVersionRequest;
import com.tuttime.common.proto.GetAppVersionResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin Service gRPC Implementation
 * 
 * <p>Provides gRPC endpoints for admin service operations.
 * 
 * <p>Note: Mining formula configuration is stored in the system_config table
 * (previously in mining_formula_config which has been merged).
 */
@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AdminGrpcService extends AdminServiceGrpc.AdminServiceImplBase {

    private final AdminMiningFormulaService miningFormulaService;
    private final AppVersionService appVersionService;

    @Override
    @Transactional(readOnly = true)
    public void getMiningFormulaConfig(
            GetMiningFormulaConfigRequest request,
            StreamObserver<GetMiningFormulaConfigResponse> responseObserver) {
        
        log.debug("gRPC: GetMiningFormulaConfig request");
        
        try {
            MiningFormulaConfigResponse config = miningFormulaService.getMiningFormulaConfig();
            
            GetMiningFormulaConfigResponse response = GetMiningFormulaConfigResponse.newBuilder()
                    .setBaseSpeedUnverified(config.getBaseSpeedUnverified() != null 
                            ? config.getBaseSpeedUnverified().toString() : "0.3")
                    .setBaseSpeedVerified(config.getBaseSpeedVerified() != null 
                            ? config.getBaseSpeedVerified().toString() : "1.0")
                    .setFriendBonusPerFriend(config.getFriendBonusPerFriend() != null 
                            ? config.getFriendBonusPerFriend().toString() : "0.5")
                    .setMaxFriends(config.getMaxFriends() != null ? config.getMaxFriends() : 10)
                    .setMaxFriendBonus(config.getMaxFriendBonus() != null 
                            ? config.getMaxFriendBonus().toString() : "5.0")
                    .setReferralBoostPerInvitation(config.getReferralBoostPerInvitation() != null 
                            ? config.getReferralBoostPerInvitation().toString() : "0.5")
                    .setBookingBonusPerBooking(config.getBookingBonusPerBooking() != null 
                            ? config.getBookingBonusPerBooking().toString() : "2.0")
                    .setBookingBoostDurationDays(config.getBookingBoostDurationDays() != null 
                            ? config.getBookingBoostDurationDays() : 15)
                    .setMaxMiningSpeed(config.getMaxMiningSpeed() != null 
                            ? config.getMaxMiningSpeed().toString() : "77.777")
                    .setMinMiningIntervalMinutes(config.getMinMiningIntervalMinutes() != null 
                            ? config.getMinMiningIntervalMinutes() : 1)
                    .setMaxMiningIntervalMinutes(config.getMaxMiningIntervalMinutes() != null 
                            ? config.getMaxMiningIntervalMinutes() : 5)
                    .setMonthlyPoolAmount(config.getMonthlyPoolAmount() != null 
                            ? config.getMonthlyPoolAmount().toString() : "50000000000.0")
                    .setIsActive(config.getIsActive() != null ? config.getIsActive() : true)
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting mining formula config", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get mining formula config: " + e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getAppVersion(
            GetAppVersionRequest request,
            StreamObserver<GetAppVersionResponse> responseObserver) {
        
        log.debug("gRPC: GetAppVersion request");
        
        try {
            AppVersionResponse version = appVersionService.getAppVersion();
            
            GetAppVersionResponse response = GetAppVersionResponse.newBuilder()
                    .setAndroidVersion(version.getAndroidVersion())
                    .setAndroidInstallLink(version.getAndroidInstallLink())
                    .setIosVersion(version.getIosVersion())
                    .setIosInstallLink(version.getIosInstallLink())
                    .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting app version", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get app version: " + e.getMessage())
                    .asRuntimeException());
        }
    }
}

