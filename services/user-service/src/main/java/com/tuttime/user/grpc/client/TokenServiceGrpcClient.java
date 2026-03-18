package com.tuttime.user.grpc.client;

import com.tuttime.common.proto.*;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

/**
 * gRPC Client for Token Service
 *
 * <p>This client provides methods to interact with Token Service via gRPC.
 */
@Slf4j
@Component
public class TokenServiceGrpcClient {

    @GrpcClient("token-service")
    private TokenServiceGrpc.TokenServiceBlockingStub tokenServiceStub;

    /**
     * Gets the APART token balance for a user.
     *
     * @param userId The user ID
     * @return BalanceResponse containing balance information
     */
    public BalanceResponse getBalance(Long userId) {
        log.info("gRPC: Calling Token Service GetBalance for userId: {}", userId);
        try {
            GetBalanceRequest request = GetBalanceRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            BalanceResponse response = tokenServiceStub.getBalance(request);
            log.info("gRPC: Successfully received GetBalance response from token-service for userId: {} - balance: {}, lockedAmount: {}, availableBalance: {}",
                    userId, response.getBalance(), response.getLockedAmount(), response.getAvailableBalance());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting balance for user: {}", userId, e);
            throw new RuntimeException("Failed to get balance: " + e.getMessage(), e);
        }
    }

    /**
     * Gets the user status (level) based on APART token balance.
     *
     * @param userId The user ID
     * @return GetUserStatusResponse containing status level and bonus information
     */
    public GetUserStatusResponse getUserStatus(Long userId) {
        log.info("gRPC: Calling Token Service GetUserStatus for userId: {}", userId);
        try {
            GetUserStatusRequest request = GetUserStatusRequest.newBuilder()
                    .setUserId(userId)
                    .build();
            GetUserStatusResponse response = tokenServiceStub.getUserStatus(request);
            log.info("gRPC: Successfully received GetUserStatus response from token-service for userId: {} - statusLevel: {}, statusBonusApPerHour: {}, premiumAutoMiningEligible: {}",
                    userId, response.getStatusLevel(), response.getStatusBonusApPerHour(), response.getPremiumAutoMiningEligible());
            return response;
        } catch (StatusRuntimeException e) {
            log.error("gRPC error getting user status for user: {}", userId, e);
            throw new RuntimeException("Failed to get user status: " + e.getMessage(), e);
        }
    }

    /**
     * Transfers tokens between users or from/to system.
     *
     * @param request TransferRequest containing transfer details
     * @return TransferResponse
     */
    public TransferResponse transferTokens(TransferRequest request) {
        log.info("gRPC: Calling Token Service TransferTokens from: {} to: {}, amount: {}", 
                request.getFromUserId(), request.getToUserId(), request.getAmount());
        try {
            return tokenServiceStub.transferTokens(request);
        } catch (StatusRuntimeException e) {
            log.error("gRPC error transferring tokens", e);
            throw new RuntimeException("Failed to transfer tokens: " + e.getMessage(), e);
        }
    }

}

