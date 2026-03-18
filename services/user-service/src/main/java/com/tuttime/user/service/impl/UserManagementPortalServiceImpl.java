package com.tuttime.user.service.impl;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.*;
import com.tuttime.common.proto.GetVideoBoostHistoryResponse;
import com.tuttime.common.proto.VideoBoostHistoryItem;
import com.tuttime.user.dto.*;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserDevice;
import com.tuttime.user.grpc.client.*;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.UserDeviceRepository;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.rest.client.RewardsServiceRestClient;
import com.tuttime.user.rest.client.TokenServiceRestClient;
import com.tuttime.user.service.UserManagementPortalService;
import com.tuttime.user.dto.ReferralInfoResponse;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * User Management Portal Service Implementation
 * 
 * <p>Provides comprehensive user management operations for admin portal.
 * Aggregates data from multiple services (user, token, mining, staking, rewards).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementPortalServiceImpl implements UserManagementPortalService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserMapper userMapper;
    private final TokenServiceGrpcClient tokenServiceGrpcClient;
    private final MiningServiceGrpcClient miningServiceGrpcClient;
    private final StakingServiceGrpcClient stakingServiceGrpcClient;
    private final RewardsServiceGrpcClient rewardsServiceGrpcClient;
    private final ConversionServiceGrpcClient conversionServiceGrpcClient;
    private final RewardsServiceRestClient rewardsServiceRestClient;
    private final TokenServiceRestClient tokenServiceRestClient;

    @Override
    public UserOverview getUserOverview(Long userId, 
                                        Integer conversionPage, Integer conversionSize,
                                        Integer transactionPage, Integer transactionSize,
                                        Integer boostPage, Integer boostSize) {
        log.debug("Getting user overview for userId: {}, conversionPage: {}, transactionPage: {}, boostPage: {}", 
                userId, conversionPage, transactionPage, boostPage);
        
        // Get user from database (in a separate transactional method to avoid holding connection during external calls)
        User user = getUserByIdTransactional(userId);
        
        // Build basic info
        UserBasicInfo basicInfo = buildBasicInfo(user);
        
        // Get balances
        UserBalances balances = buildBalances(userId);
        
        // Get mining info
        UserMiningInfo mining = buildMiningInfo(userId);
        
        // Get referral info (cached)
        UserReferralInfo referrals = buildReferralInfo(userId);
        
        // Get staking info
        UserStakingInfo staking = buildStakingInfo(userId);
        
        // Get history with pagination
        UserHistory history = buildHistory(userId, 
                conversionPage != null ? conversionPage : 0, conversionSize != null ? conversionSize : 10,
                transactionPage != null ? transactionPage : 0, transactionSize != null ? transactionSize : 10,
                boostPage != null ? boostPage : 0, boostSize != null ? boostSize : 10);
        
        // Build account status
        AccountStatus accountStatus = buildAccountStatus(user);
        
        return UserOverview.builder()
                .userId(userId)
                .basicInfo(basicInfo)
                .balances(balances)
                .mining(mining)
                .referrals(referrals)
                .staking(staking)
                .history(history)
                .accountStatus(accountStatus)
                .build();
    }

    @Override
    public ExtendedUserDetail getExtendedUserDetail(Long userId) {
        log.debug("Getting extended user detail for userId: {}", userId);
        
        // Get user from database (in a separate transactional method to avoid holding connection during external calls)
        User user = getUserByIdTransactional(userId);
        
        // Build basic info
        UserBasicInfo basicInfo = buildBasicInfo(user);
        
        // Get device info (get first active device or first device)
        List<UserDevice> devices = getUserDevicesTransactional(userId);
        DeviceInfo deviceInfo = null;
        if (!devices.isEmpty()) {
            // Prefer active device, otherwise use first device
            UserDevice device = devices.stream()
                    .filter(UserDevice::getIsActive)
                    .findFirst()
                    .orElse(devices.get(0));
            deviceInfo = mapDevice(device);
        } else {
            // Create empty device info
            deviceInfo = DeviceInfo.builder()
                    .deviceId(null)
                    .deviceType(null)
                    .deviceModel(null)
                    .osVersion(null)
                    .appVersion(null)
                    .ipAddress(null)
                    .lastSeen(null)
                    .isActive(false)
                    .isTrusted(false)
                    .build();
        }
        
        // Get balances
        UserBalances balances = buildBalances(userId);
        
        // Get mining info
        UserMiningInfo mining = buildMiningInfo(userId);
        
        // Get referral info (cached)
        UserReferralInfo referrals = buildReferralInfo(userId);
        
        // Build account status
        AccountStatus accountStatus = buildAccountStatus(user);
        
        return ExtendedUserDetail.builder()
                .basicInfo(basicInfo)
                .deviceInfo(deviceInfo)
                .balances(balances)
                .mining(mining)
                .referrals(referrals)
                .accountStatus(accountStatus)
                .build();
    }

    /**
     * Gets user by ID in a transactional context.
     * This ensures the database connection is released before making external calls.
     */
    @Transactional(readOnly = true)
    private User getUserByIdTransactional(Long userId) {
        return userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    /**
     * Gets user devices in a transactional context.
     * This ensures the database connection is released before making external calls.
     */
    @Transactional(readOnly = true)
    private List<UserDevice> getUserDevicesTransactional(Long userId) {
        return userDeviceRepository.findByUserId(userId);
    }

    private UserBasicInfo buildBasicInfo(User user) {
        List<DeviceInfo> devices = getUserDevicesTransactional(user.getUserId()).stream()
                .map(this::mapDevice)
                .collect(Collectors.toList());
        
        return UserBasicInfo.builder()
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .username(user.getUsername())
                .phone(user.getPhone())
                .telegram(user.getTelegram())
                .country(user.getCountry())
                .city(user.getCity())
                .registrationDate(user.getRegistrationDate())
                .lastActive(user.getLastActive())
                .lastLogin(user.getLastLogin())
                .devices(devices)
                .build();
    }

    private DeviceInfo mapDevice(UserDevice device) {
        return DeviceInfo.builder()
                .deviceId(device.getDeviceId())
                .deviceType(device.getDeviceType() != null ? device.getDeviceType().name() : null)
                .deviceModel(device.getDeviceModel())
                .osVersion(device.getOsVersion())
                .appVersion(device.getAppVersion())
                .ipAddress(device.getIpAddress())
                .lastSeen(device.getLastSeen())
                .isActive(device.getIsActive())
                .isTrusted(device.getIsTrusted())
                .build();
    }

    private UserBalances buildBalances(Long userId) {
        try {
            // Get APART balance
            BalanceResponse apartBalance = tokenServiceGrpcClient.getBalance(userId);
            
            // Get AP balance
            ApBalanceResponse apBalance = miningServiceGrpcClient.getApBalance(userId);
            
            return UserBalances.builder()
                    .apartBalance(new BigDecimal(apartBalance.getBalance()))
                    .apBalance(new BigDecimal(apBalance.getCurrentBalance()))
                    .monthlyAccumulatedAp(new BigDecimal(apBalance.getMonthlyAccumulated()))
                    .lifetimeAccumulatedAp(new BigDecimal(apBalance.getLifetimeAccumulated()))
                    .build();
        } catch (Exception e) {
            log.error("Error getting balances for user: {}", userId, e);
            return UserBalances.builder()
                    .apartBalance(BigDecimal.ZERO)
                    .apBalance(BigDecimal.ZERO)
                    .monthlyAccumulatedAp(BigDecimal.ZERO)
                    .lifetimeAccumulatedAp(BigDecimal.ZERO)
                    .build();
        }
    }

    private UserMiningInfo buildMiningInfo(Long userId) {
        try {
            ApBalanceResponse apBalance = miningServiceGrpcClient.getApBalance(userId);
            GetMiningSpeedResponse miningSpeed = miningServiceGrpcClient.getMiningSpeed(userId);
            GetUserStatusResponse userStatus = tokenServiceGrpcClient.getUserStatus(userId);
            
            return UserMiningInfo.builder()
                    .isActive(apBalance.getMiningActive())
                    .miningSpeed(BigDecimal.valueOf(miningSpeed.getTotalSpeed()))
                    .statusLevel(userStatus.getStatusLevel())
                    .statusBonusApPerHour(new BigDecimal(userStatus.getStatusBonusApPerHour()))
                    .totalLogsCount(0L) // TODO: Get from mining service if available
                    .lastLogAt(null) // TODO: Get from mining service if available
                    .build();
        } catch (Exception e) {
            log.error("Error getting mining info for user: {}", userId, e);
            return UserMiningInfo.builder()
                    .isActive(false)
                    .miningSpeed(BigDecimal.ZERO)
                    .statusLevel("BRONZE")
                    .statusBonusApPerHour(BigDecimal.ZERO)
                    .totalLogsCount(0L)
                    .lastLogAt(null)
                    .build();
        }
    }

    private UserReferralInfo buildReferralInfo(Long userId) {
        try {
            // Get referral code via gRPC
            ReferralCodeResponse referralCode = rewardsServiceGrpcClient.getReferralCode(userId);
            
            // Get detailed referral statistics via REST
            ReferralInfoResponse referralInfo = getReferralInfo(userId);
            
            return UserReferralInfo.builder()
                    .referralCode(referralCode.getReferralCode())
                    .referralLevel(referralCode.getReferralLevel())
                    .totalReferrals(referralInfo.getTotalReferrals())
                    .activeReferrals(referralInfo.getActiveReferrals())
                    .totalEarned(referralInfo.getTotalEarned())
                    .build();
        } catch (Exception e) {
            log.error("Error getting referral info for user: {}", userId, e);
            // Fallback to gRPC only
            try {
                ReferralCodeResponse referralCode = rewardsServiceGrpcClient.getReferralCode(userId);
                return UserReferralInfo.builder()
                        .referralCode(referralCode.getReferralCode())
                        .referralLevel(referralCode.getReferralLevel())
                        .totalReferrals(0)
                        .activeReferrals(0)
                        .totalEarned(BigDecimal.ZERO)
                        .build();
            } catch (Exception ex) {
                log.error("Error getting referral code via gRPC for user: {}", userId, ex);
                return UserReferralInfo.builder()
                        .referralCode("")
                        .referralLevel(0)
                        .totalReferrals(0)
                        .activeReferrals(0)
                        .totalEarned(BigDecimal.ZERO)
                        .build();
            }
        }
    }

    private ReferralInfoResponse getReferralInfo(Long userId) {
        return rewardsServiceRestClient.getReferralInfo(userId);
    }

    private UserStakingInfo buildStakingInfo(Long userId) {
        try {
            StakingPositionsResponse stakingPositions = stakingServiceGrpcClient.getStakingPositions(userId);
            
            BigDecimal totalStaked = BigDecimal.ZERO;
            BigDecimal totalRewards = BigDecimal.ZERO;
            List<com.tuttime.user.dto.StakingPosition> positions = new ArrayList<>();
            
            for (com.tuttime.common.proto.StakingPosition protoPosition : stakingPositions.getPositionsList()) {
                BigDecimal amount = new BigDecimal(protoPosition.getAmount());
                BigDecimal rewards = new BigDecimal(protoPosition.getRewardsEarned());
                totalStaked = totalStaked.add(amount);
                totalRewards = totalRewards.add(rewards);
                
                positions.add(com.tuttime.user.dto.StakingPosition.builder()
                        .stakingId(protoPosition.getStakingId())
                        .poolId(protoPosition.getPoolId())
                        .amount(amount)
                        .status(protoPosition.getStatus())
                        .stakedAt(null) // Not available in proto
                        .unlockAt(null) // Not available in proto
                        .boostMultiplier(new BigDecimal(protoPosition.getBoostMultiplier()))
                        .rewardsEarned(rewards)
                        .build());
            }
            
            return UserStakingInfo.builder()
                    .totalStaked(totalStaked)
                    .activeStakes(positions.size())
                    .totalRewardsEarned(totalRewards)
                    .stakingPositions(positions)
                    .build();
        } catch (Exception e) {
            log.error("Error getting staking info for user: {}", userId, e);
            return UserStakingInfo.builder()
                    .totalStaked(BigDecimal.ZERO)
                    .activeStakes(0)
                    .totalRewardsEarned(BigDecimal.ZERO)
                    .stakingPositions(new ArrayList<>())
                    .build();
        }
    }

    private UserHistory buildHistory(Long userId, 
                                     int conversionPage, int conversionSize,
                                     int transactionPage, int transactionSize,
                                     int boostPage, int boostSize) {
        List<ConversionHistoryItem> conversions = buildConversionHistory(userId, conversionPage, conversionSize);
        List<TransactionHistoryItem> transactions = buildTransactionHistory(userId, transactionPage, transactionSize);
        List<BoostHistoryItem> boosts = buildBoostHistory(userId, boostPage, boostSize);
        
        return UserHistory.builder()
                .conversions(conversions)
                .transactions(transactions)
                .boosts(boosts)
                .build();
    }

    private List<ConversionHistoryItem> buildConversionHistory(Long userId, int page, int size) {
        try {
            GetUserConversionsResponse response = conversionServiceGrpcClient.getUserConversions(userId, page, size);
            
            List<ConversionHistoryItem> conversions = new ArrayList<>();
            for (com.tuttime.common.proto.ConversionTask protoConversion : response.getConversionsList()) {
                conversions.add(ConversionHistoryItem.builder()
                        .conversionId(protoConversion.getConversionId())
                        .apAmount(new BigDecimal(protoConversion.getApCredits()))
                        .tokenAmount(new BigDecimal(protoConversion.getTokens()))
                        .conversionRate(protoConversion.getTokens().isEmpty() || protoConversion.getApCredits().isEmpty()
                                ? BigDecimal.ZERO
                                : new BigDecimal(protoConversion.getTokens())
                                        .divide(new BigDecimal(protoConversion.getApCredits()), 18, java.math.RoundingMode.HALF_UP))
                        .status(protoConversion.getStatus())
                        .createdAt(protoConversion.getCompletedAt() != null && !protoConversion.getCompletedAt().isEmpty()
                                ? OffsetDateTime.parse(protoConversion.getCompletedAt())
                                : OffsetDateTime.parse(protoConversion.getConversionDate() + "T00:00:00Z"))
                        .build());
            }
            return conversions;
        } catch (Exception e) {
            log.error("Error getting conversion history for user: {}", userId, e);
            return new ArrayList<>();
        }
    }

    private List<TransactionHistoryItem> buildTransactionHistory(Long userId, int page, int size) {
        try {
            Page<TransactionHistoryItem> transactionPage = tokenServiceRestClient.getTransactionHistory(userId, page, size);
            return transactionPage.getContent();
        } catch (Exception e) {
            log.error("Error getting transaction history for user: {}", userId, e);
            return new ArrayList<>();
        }
    }

    private List<BoostHistoryItem> buildBoostHistory(Long userId, int page, int size) {
        try {
            // Use gRPC instead of REST
            GetVideoBoostHistoryResponse response = miningServiceGrpcClient.getVideoBoostHistory(userId, page, size);
            
            List<BoostHistoryItem> boostHistory = new ArrayList<>();
            for (VideoBoostHistoryItem item : response.getItemsList()) {
                // Parse timestamps
                OffsetDateTime activatedAt = null;
                OffsetDateTime expiresAt = null;
                try {
                    if (item.getActivatedAt() != null && !item.getActivatedAt().isEmpty()) {
                        activatedAt = OffsetDateTime.parse(item.getActivatedAt());
                    }
                    if (item.getExpiresAt() != null && !item.getExpiresAt().isEmpty()) {
                        expiresAt = OffsetDateTime.parse(item.getExpiresAt());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse boost timestamps: activatedAt={}, expiresAt={}", 
                            item.getActivatedAt(), item.getExpiresAt());
                }
                
                BoostHistoryItem boostItem = BoostHistoryItem.builder()
                        .boostId(item.getBoostId())
                        .boostType(item.getBoostType())
                        .multiplier(new BigDecimal(item.getMultiplier()))
                        .duration(item.getDurationHours())
                        .activatedAt(activatedAt)
                        .expiresAt(expiresAt)
                        .build();
                
                boostHistory.add(boostItem);
            }
            
            return boostHistory;
        } catch (Exception e) {
            log.error("Error getting boost history for user: {}", userId, e);
            return new ArrayList<>();
        }
    }

    private AccountStatus buildAccountStatus(User user) {
        return AccountStatus.builder()
                .isActive(user.getIsActive())
                .isBlocked(user.getIsBlocked())
                .isPaused(user.getIsPaused() != null ? user.getIsPaused() : false)
                .blockedReason(user.getBlockedReason())
                .blockedAt(user.getBlockedAt())
                .kycCompleted(user.getKycCompleted())
                .kycVerifiedAt(user.getKycVerifiedAt())
                .withdrawalFrozen(user.getWithdrawalFrozen() != null ? user.getWithdrawalFrozen() : false)
                .antiFraudWatch(user.getAntiFraudWatch() != null ? user.getAntiFraudWatch() : false)
                .build();
    }

    @Override
    public void resetMining(Long userId, ResetMiningRequest request) {
        log.info("Resetting mining for user: {}, reason: {}", userId, request.getReason());
        
        // Validate user exists (in a separate transactional method to avoid holding connection during external calls)
        validateUserExists(userId);
        
        try {
            if (request.getResetMonthlyAccumulated() != null && request.getResetMonthlyAccumulated()) {
                miningServiceGrpcClient.resetMonthlyAccumulated(userId);
            }
            log.info("Mining reset successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Error resetting mining for user: {}", userId, e);
            throw new RuntimeException("Failed to reset mining: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void pauseAccount(Long userId, PauseAccountRequest request) {
        log.info("Pausing account for user: {}, reason: {}", userId, request.getReason());
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setIsPaused(true);
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("Account paused successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public void unpauseAccount(Long userId) {
        log.info("Unpausing account for user: {}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setIsPaused(false);
        user.setIsActive(true);
        userRepository.save(user);
        
        log.info("Account unpaused successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public void forceKyc(Long userId, ForceKycRequest request) {
        log.info("Forcing KYC status for user: {}, completed: {}, reason: {}", 
                userId, request.getCompleted(), request.getReason());
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setKycCompleted(request.getCompleted());
        if (request.getCompleted()) {
            user.setKycVerifiedAt(OffsetDateTime.now(ZoneOffset.UTC));
        } else {
            user.setKycVerifiedAt(null);
        }
        userRepository.save(user);
        
        log.info("KYC status updated successfully for user: {}", userId);
    }

    @Override
    public BonusResponse addBonus(Long userId, AddBonusRequest request) {
        log.info("Adding bonus for user: {}, type: {}, amount: {}, reason: {}", 
                userId, request.getBonusType(), request.getAmount(), request.getReason());
        
        // Validate user exists (in a separate transactional method to avoid holding connection during external calls)
        validateUserExists(userId);
        
        BigDecimal newBalance;
        try {
            if ("AP".equals(request.getBonusType())) {
                CreditApResponse response = miningServiceGrpcClient.creditAp(
                        userId, 
                        request.getAmount().toString(), 
                        "ADMIN_BONUS"
                );
                newBalance = new BigDecimal(response.getNewBalance());
            } else if ("APART".equals(request.getBonusType())) {
                // Transfer from system (user 0) to user
                TransferRequest transferRequest = TransferRequest.newBuilder()
                        .setFromUserId(0) // System user
                        .setToUserId(userId)
                        .setAmount(request.getAmount().toString())
                        .setType("MINT")
                        .setReferenceType("ADMIN_BONUS")
                        .setReferenceId("")
                        .build();
                tokenServiceGrpcClient.transferTokens(transferRequest);
                
                // Get updated balance
                BalanceResponse balance = tokenServiceGrpcClient.getBalance(userId);
                newBalance = new BigDecimal(balance.getBalance());
            } else {
                throw new IllegalArgumentException("Invalid bonus type: " + request.getBonusType());
            }
            
            return BonusResponse.builder()
                    .bonusId(System.currentTimeMillis()) // Use timestamp as ID
                    .bonusType(request.getBonusType())
                    .amount(request.getAmount())
                    .newBalance(newBalance)
                    .addedAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build();
        } catch (Exception e) {
            log.error("Error adding bonus for user: {}", userId, e);
            throw new RuntimeException("Failed to add bonus: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that a user exists in a transactional context.
     * This ensures the database connection is released before making external calls.
     */
    @Transactional(readOnly = true)
    private void validateUserExists(Long userId) {
        userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
    }

    @Override
    @Transactional
    public void freezeWithdrawal(Long userId, FreezeWithdrawalRequest request) {
        log.info("Freezing withdrawals for user: {}, reason: {}", userId, request.getReason());
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setWithdrawalFrozen(true);
        userRepository.save(user);
        
        log.info("Withdrawals frozen successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public void unfreezeWithdrawal(Long userId) {
        log.info("Unfreezing withdrawals for user: {}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setWithdrawalFrozen(false);
        userRepository.save(user);
        
        log.info("Withdrawals unfrozen successfully for user: {}", userId);
    }

    @Override
    @Transactional
    public void markAntiFraud(Long userId, MarkAntiFraudRequest request) {
        log.info("Marking user for anti-fraud watch: {}, reason: {}, severity: {}", 
                userId, request.getReason(), request.getSeverity());
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setAntiFraudWatch(true);
        userRepository.save(user);
        
        log.info("User marked for anti-fraud watch successfully: {}", userId);
    }

    @Override
    @Transactional
    public void unmarkAntiFraud(Long userId) {
        log.info("Removing anti-fraud watch from user: {}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        user.setAntiFraudWatch(false);
        userRepository.save(user);
        
        log.info("Anti-fraud watch removed successfully for user: {}", userId);
    }
}

