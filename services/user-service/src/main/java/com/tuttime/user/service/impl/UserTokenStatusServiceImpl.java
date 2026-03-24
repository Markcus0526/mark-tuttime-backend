package com.tuttime.user.service.impl;

import com.tuttime.common.proto.BalanceResponse;
import com.tuttime.common.proto.GetUserStatusResponse;
import com.tuttime.common.proto.TokenServiceProto.*;
import com.tuttime.user.dto.ConversionInfo;
import com.tuttime.user.dto.UserTokenHoldingStatus;
import com.tuttime.user.grpc.client.ConversionServiceGrpcClient;
import com.tuttime.user.grpc.client.TokenServiceGrpcClient;
import com.tuttime.user.service.UserTokenStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implementation of User Token Status Service
 * 
 * <p>This service integrates with Token Service (via gRPC) and Conversion Service (via REST)
 * to provide comprehensive token holding status information for users.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserTokenStatusServiceImpl implements UserTokenStatusService {

    private final TokenServiceGrpcClient tokenServiceGrpcClient;
    private final ConversionServiceGrpcClient conversionServiceGrpcClient;

    // Status level thresholds from Token Service (matching StatusService)
    private static final BigDecimal STATUS_SILVER_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal STATUS_GOLD_THRESHOLD = new BigDecimal("100000");
    private static final BigDecimal STATUS_PLATINUM_THRESHOLD = new BigDecimal("200000");
    private static final BigDecimal STATUS_DIAMOND_THRESHOLD = new BigDecimal("500000");
    private static final BigDecimal STATUS_ELITE_THRESHOLD = new BigDecimal("1000000");

    @Override
    public UserTokenHoldingStatus getUserTokenHoldingStatus(Long userId) {
        log.info("Getting token holding status for user: {}", userId);

        try {
            // Get balance from Token Service via gRPC
            BalanceResponse balanceResponse = tokenServiceGrpcClient.getBalance(userId);
            BigDecimal currentBalance = new BigDecimal(balanceResponse.getBalance());

            // Get user status from Token Service (includes level calculation)
            GetUserStatusResponse statusResponse = tokenServiceGrpcClient.getUserStatus(userId);
            String currentLevel = statusResponse.getStatusLevel();

            // Get conversion info from Conversion Service via gRPC
            ConversionInfo conversionInfo = conversionServiceGrpcClient.getConversionInfo(userId);

            // Calculate APARTs needed for next level
            BigDecimal apartsForNextLevel = calculateApartsForNextLevel(currentLevel, currentBalance);

            return UserTokenHoldingStatus.builder()
                    .currentApartBalance(currentBalance)
                    .thisMonthForecastAmount(conversionInfo.getThisMonthForecastAmount())
                    .currentLevel(currentLevel)
                    .apartsForNextLevel(apartsForNextLevel)
                    .nextConversionTime(conversionInfo.getNextConversionTime())
                    .lastMonthApartConversion(conversionInfo.getLastMonthApartConversion())
                    .build();

        } catch (Exception e) {
            log.error("Error getting token holding status for user: {}", userId, e);
            // Return empty status on error (graceful degradation)
            return UserTokenHoldingStatus.builder()
                    .currentApartBalance(BigDecimal.ZERO)
                    .thisMonthForecastAmount(BigDecimal.ZERO)
                    .currentLevel("BRONZE")
                    .apartsForNextLevel(BigDecimal.ZERO)
                    .nextConversionTime(null)
                    .lastMonthApartConversion(BigDecimal.ZERO)
                    .build();
        }
    }

    /**
     * Calculates APARTs needed for next level based on Token Service thresholds.
     * 
     * <p>Status thresholds:
     * <ul>
     *   <li>BRONZE: < 50,000 APART</li>
     *   <li>SILVER: 50,000 - 99,999 APART</li>
     *   <li>GOLD: 100,000 - 199,999 APART</li>
     *   <li>PLATINUM: 200,000 - 499,999 APART</li>
     *   <li>DIAMOND: 500,000 - 999,999 APART</li>
     *   <li>ELITE: 1,000,000+ APART</li>
     * </ul>
     * 
     * @param currentLevel Current level as String (from Token Service)
     * @param currentBalance Current APART balance
     * @return APARTs needed for next level
     */
    private BigDecimal calculateApartsForNextLevel(String currentLevel, BigDecimal currentBalance) {
        BigDecimal nextLevelThreshold;
        
        switch (currentLevel.toUpperCase()) {
            case "BRONZE":
                nextLevelThreshold = STATUS_SILVER_THRESHOLD;
                break;
            case "SILVER":
                nextLevelThreshold = STATUS_GOLD_THRESHOLD;
                break;
            case "GOLD":
                nextLevelThreshold = STATUS_PLATINUM_THRESHOLD;
                break;
            case "PLATINUM":
                nextLevelThreshold = STATUS_DIAMOND_THRESHOLD;
                break;
            case "DIAMOND":
                nextLevelThreshold = STATUS_ELITE_THRESHOLD;
                break;
            case "ELITE":
                return BigDecimal.ZERO; // Max level reached
            default:
                log.warn("Unknown status level: {}, defaulting to BRONZE threshold", currentLevel);
                nextLevelThreshold = STATUS_SILVER_THRESHOLD;
        }

        BigDecimal needed = nextLevelThreshold.subtract(currentBalance);
        return needed.compareTo(BigDecimal.ZERO) > 0 ? needed : BigDecimal.ZERO;
    }
}

