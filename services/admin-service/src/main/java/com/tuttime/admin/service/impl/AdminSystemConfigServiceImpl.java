package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.StatusLevelRequest;
import com.tuttime.admin.dto.StatusLevelResponse;
import com.tuttime.admin.dto.StakingPoolResponse;
import com.tuttime.admin.dto.StakingTierResponse;
import com.tuttime.admin.dto.SystemConfigResponse;
import com.tuttime.admin.dto.UpdateSystemConfigRequest;
import com.tuttime.admin.entity.SystemConfig;
import com.tuttime.admin.repository.StatusLevelRepository;
import com.tuttime.admin.repository.StakingTierRepository;
import com.tuttime.admin.repository.SystemConfigRepository;
import com.tuttime.admin.service.AdminStakingService;
import com.tuttime.admin.service.AdminSystemConfigService;
import com.tuttime.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin System Config Service Implementation
 * 
 * <p>TODO: Store system configuration in database or config service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSystemConfigServiceImpl implements AdminSystemConfigService {

    private final StakingTierRepository stakingTierRepository;
    @Deprecated
    private final StatusLevelRepository statusLevelRepository; // Deprecated - no longer used, kept for backward compatibility
    private final SystemConfigRepository systemConfigRepository;
    // TODO: Uncomment when staking-service is deployed
    // private final AdminStakingService stakingService; // For getting pools from staking-service
    // TODO: Uncomment when token-service is deployed
    // private final com.tuttime.admin.rest.client.TokenServiceRestClient tokenServiceRestClient; // For status level management

    // Default values from requirements
    private static final BigDecimal DEFAULT_BASE_MINING_SPEED = new BigDecimal("0.3"); // 0.3 AP/hr
    private static final BigDecimal DEFAULT_MAX_MINING_SPEED = new BigDecimal("77.777"); // 77.777 AP/hr
    private static final Integer DEFAULT_MIN_MINING_INTERVAL_MINUTES = 1; // 1 minute
    private static final Integer DEFAULT_MAX_MINING_INTERVAL_MINUTES = 5; // 5 minutes
    private static final BigDecimal DEFAULT_MONTHLY_POOL_AMOUNT = new BigDecimal("50000000000"); // 50B APART
    
    // Rewards Boost Parameters Defaults
    // Note: DEFAULT_REFERRAL_BOOST_PER_INVITATION is now in system_config table (merged from mining_formula_config)
    private static final Integer DEFAULT_MAX_REFERRAL_BOOSTS = 10; // 10 referrals per user
    private static final BigDecimal DEFAULT_BOOKING_BOOST_BASE = new BigDecimal("2"); // +2 AP/hr
    private static final Integer DEFAULT_BOOKING_BOOST_DURATION_DAYS = 15; // 15 days
    private static final BigDecimal DEFAULT_RENTAL_BOOST_MULTIPLIER = new BigDecimal("2"); // * 2 AP/hr
    private static final Integer DEFAULT_RENTAL_BOOST_DURATION_DAYS = 2; // 2 days
    
    // Extra Parameters Defaults
    private static final BigDecimal DEFAULT_WITHDRAWAL_PLATFORM_FEE_PERCENT = BigDecimal.ZERO; // 0% currently
    private static final BigDecimal DEFAULT_DEPOSIT_MINIMUM_AMOUNT = null; // No minimum by default
    private static final BigDecimal DEFAULT_WITHDRAWAL_MINIMUM_AMOUNT = null; // No minimum by default
    private static final BigDecimal DEFAULT_WITHDRAWAL_MAXIMUM_AMOUNT = null; // No maximum by default
    private static final Integer DEFAULT_WITHDRAWAL_DAILY_LIMIT = null; // No daily limit by default
    
    // Legacy defaults (for backward compatibility)
    @Deprecated
    private static final BigDecimal DEFAULT_MONTHLY_POOL = new BigDecimal("50000000"); // 50M APART (deprecated)
    @Deprecated
    private static final Integer DEFAULT_MAX_REFERRAL_BONUS = 2; // +2 AP/hour max (deprecated)
    @Deprecated
    private static final BigDecimal DEFAULT_REFERRAL_BONUS_PER_ACTIVE = new BigDecimal("0.2"); // +0.2 AP/hour per active (deprecated)

    @Override
    @Transactional(readOnly = true)
    public SystemConfigResponse getSystemConfig() {
        log.info("=== SERVICE STEP 1: AdminSystemConfigServiceImpl.getSystemConfig() START ===");
        log.info("=== SERVICE STEP 2: Getting system configuration from database ===");
        
        // Get system config from database, or use defaults if not exists
        SystemConfig config;
        try {
            log.info("=== SERVICE STEP 3: Calling systemConfigRepository.getSystemConfig() ===");
            config = systemConfigRepository.getSystemConfig()
                    .orElseGet(() -> {
                        log.info("=== SERVICE STEP 3a: System config not found in database, using defaults ===");
                        return SystemConfig.builder()
                                .configId(1L)
                                .baseMiningSpeed(DEFAULT_BASE_MINING_SPEED)
                                .maxMiningSpeed(DEFAULT_MAX_MINING_SPEED)
                                .minMiningIntervalMinutes(DEFAULT_MIN_MINING_INTERVAL_MINUTES)
                                .maxMiningIntervalMinutes(DEFAULT_MAX_MINING_INTERVAL_MINUTES)
                                .monthlyPoolAmount(DEFAULT_MONTHLY_POOL_AMOUNT)
                                .maxReferralBoosts(DEFAULT_MAX_REFERRAL_BOOSTS)
                                .bookingBoostBase(DEFAULT_BOOKING_BOOST_BASE)
                                .bookingBoostDurationDays(DEFAULT_BOOKING_BOOST_DURATION_DAYS)
                                .rentalBoostMultiplier(DEFAULT_RENTAL_BOOST_MULTIPLIER)
                                .rentalBoostDurationDays(DEFAULT_RENTAL_BOOST_DURATION_DAYS)
                                .withdrawalPlatformFeePercent(DEFAULT_WITHDRAWAL_PLATFORM_FEE_PERCENT)
                                .depositMinimumAmount(DEFAULT_DEPOSIT_MINIMUM_AMOUNT)
                                .withdrawalMinimumAmount(DEFAULT_WITHDRAWAL_MINIMUM_AMOUNT)
                                .withdrawalMaximumAmount(DEFAULT_WITHDRAWAL_MAXIMUM_AMOUNT)
                                .withdrawalDailyLimit(DEFAULT_WITHDRAWAL_DAILY_LIMIT)
                                .monthlyPool(DEFAULT_MONTHLY_POOL)
                                .maxReferralBonus(DEFAULT_MAX_REFERRAL_BONUS)
                                .referralBonusPerActive(DEFAULT_REFERRAL_BONUS_PER_ACTIVE)
                                .build();
                    });
            log.info("=== SERVICE STEP 4: Config retrieved from database - configId: {} ===", 
                    config != null ? config.getConfigId() : "null");
            log.debug("System config retrieved from database: configId={}", config.getConfigId());
        } catch (InvalidDataAccessResourceUsageException e) {
            // Table doesn't exist - log as debug and use defaults
            log.debug("System config table not found, using defaults: {}", e.getMessage());
            config = SystemConfig.builder()
                    .configId(1L)
                    .baseMiningSpeed(DEFAULT_BASE_MINING_SPEED)
                    .maxMiningSpeed(DEFAULT_MAX_MINING_SPEED)
                    .minMiningIntervalMinutes(DEFAULT_MIN_MINING_INTERVAL_MINUTES)
                    .maxMiningIntervalMinutes(DEFAULT_MAX_MINING_INTERVAL_MINUTES)
                    .monthlyPoolAmount(DEFAULT_MONTHLY_POOL_AMOUNT)
                    .maxReferralBoosts(DEFAULT_MAX_REFERRAL_BOOSTS)
                    .bookingBoostBase(DEFAULT_BOOKING_BOOST_BASE)
                    .bookingBoostDurationDays(DEFAULT_BOOKING_BOOST_DURATION_DAYS)
                    .rentalBoostMultiplier(DEFAULT_RENTAL_BOOST_MULTIPLIER)
                    .rentalBoostDurationDays(DEFAULT_RENTAL_BOOST_DURATION_DAYS)
                    .withdrawalPlatformFeePercent(DEFAULT_WITHDRAWAL_PLATFORM_FEE_PERCENT)
                    .depositMinimumAmount(DEFAULT_DEPOSIT_MINIMUM_AMOUNT)
                    .withdrawalMinimumAmount(DEFAULT_WITHDRAWAL_MINIMUM_AMOUNT)
                    .withdrawalMaximumAmount(DEFAULT_WITHDRAWAL_MAXIMUM_AMOUNT)
                    .withdrawalDailyLimit(DEFAULT_WITHDRAWAL_DAILY_LIMIT)
                    .monthlyPool(DEFAULT_MONTHLY_POOL)
                    .maxReferralBonus(DEFAULT_MAX_REFERRAL_BONUS)
                    .referralBonusPerActive(DEFAULT_REFERRAL_BONUS_PER_ACTIVE)
                    .build();
        } catch (Exception e) {
            String stackTraceSummary = com.tuttime.common.exception.StackTraceUtil.getStackTraceSummary(e, 3);
            log.error("Error retrieving system config from database, using defaults: {}, stackTrace={}", e.getMessage(), stackTraceSummary);
            // Use defaults if database query fails
            config = SystemConfig.builder()
                    .configId(1L)
                    .baseMiningSpeed(DEFAULT_BASE_MINING_SPEED)
                    .maxMiningSpeed(DEFAULT_MAX_MINING_SPEED)
                    .minMiningIntervalMinutes(DEFAULT_MIN_MINING_INTERVAL_MINUTES)
                    .maxMiningIntervalMinutes(DEFAULT_MAX_MINING_INTERVAL_MINUTES)
                    .monthlyPoolAmount(DEFAULT_MONTHLY_POOL_AMOUNT)
                    .maxReferralBoosts(DEFAULT_MAX_REFERRAL_BOOSTS)
                    .bookingBoostBase(DEFAULT_BOOKING_BOOST_BASE)
                    .bookingBoostDurationDays(DEFAULT_BOOKING_BOOST_DURATION_DAYS)
                    .rentalBoostMultiplier(DEFAULT_RENTAL_BOOST_MULTIPLIER)
                    .rentalBoostDurationDays(DEFAULT_RENTAL_BOOST_DURATION_DAYS)
                    .withdrawalPlatformFeePercent(DEFAULT_WITHDRAWAL_PLATFORM_FEE_PERCENT)
                    .depositMinimumAmount(DEFAULT_DEPOSIT_MINIMUM_AMOUNT)
                    .withdrawalMinimumAmount(DEFAULT_WITHDRAWAL_MINIMUM_AMOUNT)
                    .withdrawalMaximumAmount(DEFAULT_WITHDRAWAL_MAXIMUM_AMOUNT)
                    .withdrawalDailyLimit(DEFAULT_WITHDRAWAL_DAILY_LIMIT)
                    .monthlyPool(DEFAULT_MONTHLY_POOL)
                    .maxReferralBonus(DEFAULT_MAX_REFERRAL_BONUS)
                    .referralBonusPerActive(DEFAULT_REFERRAL_BONUS_PER_ACTIVE)
                    .build();
        }

        // TODO: Uncomment when staking-service is deployed
        // Get staking pools from staking-service
        /*
        log.info("=== SERVICE STEP 5: Getting staking pools from staking-service ===");
        List<StakingPoolResponse> stakingPools;
        List<StakingTierResponse> stakingTiers; // Legacy field for backward compatibility
        try {
            stakingPools = stakingService.listStakingPools();
            log.info("=== SERVICE STEP 5a: Retrieved {} staking pools ===", 
                    stakingPools != null ? stakingPools.size() : 0);
            // Map pools to legacy StakingTierResponse for backward compatibility
            stakingTiers = stakingPools.stream()
                    .map(pool -> StakingTierResponse.builder()
                            .poolId(pool.getPoolId())
                            .poolName(pool.getPoolName())
                            .poolType(pool.getPoolType())
                            .minStakeAmount(pool.getMinStakeAmount())
                            .maxStakeAmount(pool.getMaxStakeAmount())
                            .lockPeriodDays(pool.getLockPeriodDays())
                            .apyPercentage(pool.getApyPercentage())
                            .boostMultiplier(pool.getBoostMultiplier())
                            .isActive(pool.getIsActive())
                            .description(pool.getDescription())
                            .createdAt(pool.getCreatedAt())
                            .updatedAt(pool.getUpdatedAt())
                            // Legacy fields
                            .tierId(pool.getPoolId())
                            .tierName(pool.getPoolName())
                            .apBoostPerHour(pool.getBoostMultiplier())
                            .displayOrder(0)
                            .createdBy(null)
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // Check if it's a connection error
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Staking-service unavailable when retrieving pools, using empty list: {}", e.getMessage());
            } else {
                log.error("Error retrieving staking pools from staking-service, using empty list: {}", e.getMessage());
            }
            stakingPools = List.of();
            stakingTiers = List.of();
        }
        */
        // Staking service not deployed - return empty lists
        List<StakingPoolResponse> stakingPools = List.of();
        List<StakingTierResponse> stakingTiers = List.of();
        log.info("=== SERVICE STEP 5: Staking service not deployed, using empty lists ===");

        // TODO: Uncomment when token-service is deployed
        // Get active status levels from token-service
        /*
        log.info("=== SERVICE STEP 6: Getting status levels from token-service ===");
        List<StatusLevelResponse> statusLevels;
        try {
            List<Map<String, Object>> levelsData = tokenServiceRestClient.getAllStatusLevels();
            log.info("=== SERVICE STEP 6a: Retrieved {} status levels ===", 
                    levelsData != null ? levelsData.size() : 0);
            statusLevels = levelsData.stream()
                    .map(this::mapToStatusLevelResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            // TokenServiceRestClient should handle connection errors gracefully, but catch any remaining exceptions
            if (e.getMessage() != null && (e.getMessage().contains("Connection refused") || 
                    e.getMessage().contains("Connection timed out") ||
                    e.getMessage().contains("Service Unavailable"))) {
                log.warn("Token-service unavailable when retrieving status levels, using empty list: {}", e.getMessage());
            } else {
                log.error("Error retrieving status levels from token-service, using empty list: {}", e.getMessage());
            }
            statusLevels = List.of();
        }
        */
        // Token service not deployed - return empty list
        List<StatusLevelResponse> statusLevels = List.of();
        log.info("=== SERVICE STEP 6: Token service not deployed, using empty list ===");

        log.info("=== SERVICE STEP 7: Building SystemConfigResponse ===");
        try {
            SystemConfigResponse response = SystemConfigResponse.builder()
                    .baseMiningSpeed(config.getBaseMiningSpeed() != null ? config.getBaseMiningSpeed() : DEFAULT_BASE_MINING_SPEED)
                    .maxMiningSpeed(config.getMaxMiningSpeed() != null ? config.getMaxMiningSpeed() : DEFAULT_MAX_MINING_SPEED)
                    .minMiningIntervalMinutes(config.getMinMiningIntervalMinutes() != null ? config.getMinMiningIntervalMinutes() : DEFAULT_MIN_MINING_INTERVAL_MINUTES)
                    .maxMiningIntervalMinutes(config.getMaxMiningIntervalMinutes() != null ? config.getMaxMiningIntervalMinutes() : DEFAULT_MAX_MINING_INTERVAL_MINUTES)
                    .monthlyPoolAmount(config.getMonthlyPoolAmount() != null ? config.getMonthlyPoolAmount() : DEFAULT_MONTHLY_POOL_AMOUNT)
                    // Rewards Boost Parameters
                    // Note: referralBoostPerInvitation is now in system_config table (merged from mining_formula_config) - use AdminMiningFormulaService API instead
                    .maxReferralBoosts(config.getMaxReferralBoosts() != null ? config.getMaxReferralBoosts() : DEFAULT_MAX_REFERRAL_BOOSTS)
                    .bookingBoostBase(config.getBookingBoostBase() != null ? config.getBookingBoostBase() : DEFAULT_BOOKING_BOOST_BASE)
                    .bookingBoostDurationDays(config.getBookingBoostDurationDays() != null ? config.getBookingBoostDurationDays() : DEFAULT_BOOKING_BOOST_DURATION_DAYS)
                    .rentalBoostMultiplier(config.getRentalBoostMultiplier() != null ? config.getRentalBoostMultiplier() : DEFAULT_RENTAL_BOOST_MULTIPLIER)
                    .rentalBoostDurationDays(config.getRentalBoostDurationDays() != null ? config.getRentalBoostDurationDays() : DEFAULT_RENTAL_BOOST_DURATION_DAYS)
                    // Extra Parameters
                    .withdrawalPlatformFeePercent(config.getWithdrawalPlatformFeePercent() != null ? config.getWithdrawalPlatformFeePercent() : DEFAULT_WITHDRAWAL_PLATFORM_FEE_PERCENT)
                    .depositMinimumAmount(config.getDepositMinimumAmount())
                    .withdrawalMinimumAmount(config.getWithdrawalMinimumAmount())
                    .withdrawalMaximumAmount(config.getWithdrawalMaximumAmount())
                    .withdrawalDailyLimit(config.getWithdrawalDailyLimit())
                    // Legacy fields for backward compatibility
                    .monthlyPool(config.getMonthlyPool() != null ? config.getMonthlyPool() : DEFAULT_MONTHLY_POOL)
                    .maxReferralBonus(config.getMaxReferralBonus() != null ? config.getMaxReferralBonus() : DEFAULT_MAX_REFERRAL_BONUS)
                    .referralBonusPerActive(config.getReferralBonusPerActive() != null ? config.getReferralBonusPerActive() : DEFAULT_REFERRAL_BONUS_PER_ACTIVE)
                    .stakingPools(stakingPools != null ? stakingPools : List.of())
                    .stakingTiers(stakingTiers != null ? stakingTiers : List.of()) // Legacy field
                    .statusLevels(statusLevels != null ? statusLevels : List.of())
                    .build();
            
            // Validate response was built correctly
            if (response == null) {
                log.error("System config response is null after building");
                throw new BusinessException("Failed to build system configuration response");
            }
            
            // Validate critical fields are set
            if (response.getBaseMiningSpeed() == null) {
                log.warn("Base mining speed is null, setting default");
                response.setBaseMiningSpeed(DEFAULT_BASE_MINING_SPEED);
            }
            if (response.getMaxMiningSpeed() == null) {
                log.warn("Max mining speed is null, setting default");
                response.setMaxMiningSpeed(DEFAULT_MAX_MINING_SPEED);
            }
            
            log.info("=== SERVICE STEP 8: System config response built successfully. Has {} staking pools, {} staking tiers, {} status levels. baseMiningSpeed: {}, maxMiningSpeed: {} ===", 
                    response.getStakingPools() != null ? response.getStakingPools().size() : 0,
                    response.getStakingTiers() != null ? response.getStakingTiers().size() : 0,
                    response.getStatusLevels() != null ? response.getStatusLevels().size() : 0,
                    response.getBaseMiningSpeed(),
                    response.getMaxMiningSpeed());
            log.info("=== SERVICE STEP 9: AdminSystemConfigServiceImpl.getSystemConfig() END - Returning response ===");
            return response;
        } catch (BusinessException e) {
            log.error("Business exception building system config response: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error building system config response: {}", e.getMessage(), e);
            throw new BusinessException("Failed to retrieve system configuration: " + e.getMessage());
        }
    }

    // ============================================================================
    // Status Level Management
    // ============================================================================

    // TODO: Uncomment when token-service is deployed
    /*
    @Override
    public List<StatusLevelResponse> getAllStatusLevels() {
        log.debug("Getting all status levels from token-service");
        try {
            List<Map<String, Object>> levelsData = tokenServiceRestClient.getAllStatusLevels();
            
            // Debug: Log the first level's keys to see what fields we're receiving
            if (!levelsData.isEmpty()) {
                Map<String, Object> firstLevel = levelsData.get(0);
                log.debug("Sample status level data keys from token-service: {}", firstLevel.keySet());
            }
            
            // Map from token-service response to admin-service DTO
            return levelsData.stream()
                    .map(this::mapToStatusLevelResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting status levels from token-service", e);
            throw new BusinessException("Failed to get status levels: " + e.getMessage());
        }
    }

    @Override
    public StatusLevelResponse getStatusLevelByLevelName(String levelName) {
        log.debug("Getting status level by name from token-service: {}", levelName);
        try {
            Map<String, Object> levelData = tokenServiceRestClient.getStatusLevelByLevelName(levelName);
            return mapToStatusLevelResponse(levelData);
        } catch (Exception e) {
            log.error("Error getting status level from token-service: levelName={}", levelName, e);
            throw new BusinessException("Status level not found: " + levelName);
        }
    }

    @Override
    public StatusLevelResponse createStatusLevel(StatusLevelRequest request, Long createdBy) {
        log.info("Creating status level in token-service: {}", request.getLevelName());
        try {
            // Convert request to Map for REST client
            Map<String, Object> createRequest = new HashMap<>();
            createRequest.put("levelName", request.getLevelName());
            createRequest.put("minApartBalance", request.getMinApartBalance());
            createRequest.put("apBoostPerHour", request.getApBoostPerHour());
            createRequest.put("includesProAutoMining", request.getIncludesProAutoMining() != null ? request.getIncludesProAutoMining() : false);
            createRequest.put("displayOrder", request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
            createRequest.put("isActive", request.getIsActive() != null ? request.getIsActive() : true);
            if (request.getDescription() != null) {
                createRequest.put("description", request.getDescription());
            }
            
            Map<String, Object> levelData = tokenServiceRestClient.createStatusLevel(createRequest);
            log.info("Status level created successfully in token-service: {}", levelData.get("levelName"));
            return mapToStatusLevelResponse(levelData);
        } catch (Exception e) {
            log.error("Error creating status level in token-service: levelName={}", request.getLevelName(), e);
            throw new BusinessException("Failed to create status level: " + e.getMessage());
        }
    }

    @Override
    public StatusLevelResponse updateStatusLevel(String levelName, StatusLevelRequest request) {
        log.info("Updating status level in token-service: {}", levelName);
        try {
            // Convert request to Map for REST client (only include non-null fields for partial updates)
            java.util.Map<String, Object> updateRequest = new java.util.HashMap<>();
            if (request.getLevelName() != null) {
                updateRequest.put("levelName", request.getLevelName());
            }
            if (request.getMinApartBalance() != null) {
                updateRequest.put("minApartBalance", request.getMinApartBalance());
            }
            if (request.getApBoostPerHour() != null) {
                updateRequest.put("apBoostPerHour", request.getApBoostPerHour());
            }
            if (request.getIncludesProAutoMining() != null) {
                updateRequest.put("includesProAutoMining", request.getIncludesProAutoMining());
            }
            if (request.getDisplayOrder() != null) {
                updateRequest.put("displayOrder", request.getDisplayOrder());
            }
            if (request.getIsActive() != null) {
                updateRequest.put("isActive", request.getIsActive());
            }
            if (request.getDescription() != null) {
                updateRequest.put("description", request.getDescription());
            }
            
            Map<String, Object> levelData = tokenServiceRestClient.updateStatusLevel(levelName, updateRequest);
            log.info("Status level updated successfully in token-service: {}", levelData.get("levelName"));
            return mapToStatusLevelResponse(levelData);
        } catch (Exception e) {
            log.error("Error updating status level in token-service: levelName={}", levelName, e);
            throw new BusinessException("Failed to update status level: " + e.getMessage());
        }
    }

    @Override
    public void deleteStatusLevel(String levelName) {
        log.info("Deleting status level in token-service: {}", levelName);
        try {
            tokenServiceRestClient.deleteStatusLevel(levelName);
            log.info("Status level deleted successfully in token-service: {}", levelName);
        } catch (Exception e) {
            log.error("Error deleting status level in token-service: levelName={}", levelName, e);
            throw new BusinessException("Failed to delete status level: " + e.getMessage());
        }
    }
    */
    
    // Token service not deployed - return empty/default responses
    @Override
    public List<StatusLevelResponse> getAllStatusLevels() {
        log.warn("Token service not deployed - returning empty status levels list");
        return List.of();
    }

    @Override
    public StatusLevelResponse getStatusLevelByLevelName(String levelName) {
        log.warn("Token service not deployed - status level not found: {}", levelName);
        throw new BusinessException("Token service not deployed - status level operations unavailable");
    }

    @Override
    public StatusLevelResponse createStatusLevel(StatusLevelRequest request, Long createdBy) {
        log.warn("Token service not deployed - cannot create status level");
        throw new BusinessException("Token service not deployed - status level operations unavailable");
    }

    @Override
    public StatusLevelResponse updateStatusLevel(String levelName, StatusLevelRequest request) {
        log.warn("Token service not deployed - cannot update status level");
        throw new BusinessException("Token service not deployed - status level operations unavailable");
    }

    @Override
    public void deleteStatusLevel(String levelName) {
        log.warn("Token service not deployed - cannot delete status level");
        throw new BusinessException("Token service not deployed - status level operations unavailable");
    }
    
    /**
     * Map token-service status level data to admin-service DTO
     */
    private StatusLevelResponse mapToStatusLevelResponse(Map<String, Object> levelMap) {
        log.debug("Mapping status level data: available keys = {}", levelMap.keySet());
        
        Long levelId = getLong(levelMap, "levelId");
        String levelName = getString(levelMap, "levelName");
        BigDecimal minApartBalance = getBigDecimal(levelMap, "minApartBalance");
        BigDecimal apBoostPerHour = getBigDecimal(levelMap, "apBoostPerHour");
        Boolean includesProAutoMining = getBoolean(levelMap, "includesProAutoMining");
        Integer displayOrder = getInteger(levelMap, "displayOrder");
        Boolean isActive = getBoolean(levelMap, "isActive");
        String description = getString(levelMap, "description");
        Long createdBy = getLong(levelMap, "createdBy");
        
        // Parse datetime - handle both OffsetDateTime objects and strings
        OffsetDateTime createdAt = getOffsetDateTime(levelMap, "createdAt");
        OffsetDateTime updatedAt = getOffsetDateTime(levelMap, "updatedAt");
        
        log.debug("Mapped values: levelId={}, levelName={}, minApartBalance={}, apBoostPerHour={}, isActive={}", 
                levelId, levelName, minApartBalance, apBoostPerHour, isActive);
        
        return StatusLevelResponse.builder()
                .levelId(levelId)
                .levelName(levelName)
                .minApartBalance(minApartBalance)
                .apBoostPerHour(apBoostPerHour)
                .includesProAutoMining(includesProAutoMining)
                .displayOrder(displayOrder)
                .isActive(isActive)
                .description(description)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .createdBy(createdBy)
                .build();
    }
    
    /**
     * Helper method to safely extract OffsetDateTime from Map
     */
    private OffsetDateTime getOffsetDateTime(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof OffsetDateTime) return ((OffsetDateTime) value).withOffsetSameInstant(ZoneOffset.UTC);
        if (value instanceof String) {
            return parseDateTime((String) value);
        }
        return null;
    }
    
    /**
     * Parse datetime string to OffsetDateTime.
     * 
     * <p><b>Timezone Note:</b> Parsed dates are assumed to be in UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param dateTimeStr The datetime string to parse (ISO format, UTC)
     * @return Parsed OffsetDateTime in UTC, or null if parsing fails
     */
    private OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            // Try parsing as OffsetDateTime first
            return OffsetDateTime.parse(dateTimeStr).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse datetime string: {}", dateTimeStr, e);
            return null;
        }
    }
    
    // Helper methods to safely extract values from Map
    private BigDecimal getBigDecimal(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        if (value instanceof Number) return BigDecimal.valueOf(((Number) value).doubleValue());
        return new BigDecimal(value.toString());
    }
    
    private Long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(value.toString());
    }
    
    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return Integer.parseInt(value.toString());
    }
    
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString();
    }
    
    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }


    @Override
    @Transactional
    public SystemConfigResponse updateSystemConfig(UpdateSystemConfigRequest request, Long updatedBy) {
        log.info("Updating system configuration by adminId: {}", updatedBy);
        
        // Get existing config or create new one (singleton pattern - always ID 1)
        SystemConfig config = systemConfigRepository.getSystemConfig()
                .orElseGet(() -> {
                    log.info("Creating new system configuration (singleton)");
                    return SystemConfig.builder()
                            .configId(1L)
                            .build();
                });
        
        // Set updatedBy
        config.setUpdatedBy(updatedBy);
        
        // Update only provided fields (partial update)
        if (request.getBaseMiningSpeed() != null) {
            config.setBaseMiningSpeed(request.getBaseMiningSpeed());
        }
        if (request.getMaxMiningSpeed() != null) {
            config.setMaxMiningSpeed(request.getMaxMiningSpeed());
        }
        if (request.getMinMiningIntervalMinutes() != null) {
            config.setMinMiningIntervalMinutes(request.getMinMiningIntervalMinutes());
        }
        if (request.getMaxMiningIntervalMinutes() != null) {
            config.setMaxMiningIntervalMinutes(request.getMaxMiningIntervalMinutes());
        }
        if (request.getMonthlyPoolAmount() != null) {
            config.setMonthlyPoolAmount(request.getMonthlyPoolAmount());
        }
        // Rewards Boost Parameters
        // Note: referralBoostPerInvitation moved to MiningFormulaConfig - use MiningFormulaConfig API instead
        if (request.getMaxReferralBoosts() != null) {
            config.setMaxReferralBoosts(request.getMaxReferralBoosts());
        }
        if (request.getBookingBoostBase() != null) {
            config.setBookingBoostBase(request.getBookingBoostBase());
        }
        if (request.getBookingBoostDurationDays() != null) {
            config.setBookingBoostDurationDays(request.getBookingBoostDurationDays());
        }
        if (request.getRentalBoostMultiplier() != null) {
            config.setRentalBoostMultiplier(request.getRentalBoostMultiplier());
        }
        if (request.getRentalBoostDurationDays() != null) {
            config.setRentalBoostDurationDays(request.getRentalBoostDurationDays());
        }
        // Extra Parameters
        if (request.getWithdrawalPlatformFeePercent() != null) {
            config.setWithdrawalPlatformFeePercent(request.getWithdrawalPlatformFeePercent());
        }
        if (request.getDepositMinimumAmount() != null) {
            config.setDepositMinimumAmount(request.getDepositMinimumAmount());
        }
        if (request.getWithdrawalMinimumAmount() != null) {
            config.setWithdrawalMinimumAmount(request.getWithdrawalMinimumAmount());
        }
        if (request.getWithdrawalMaximumAmount() != null) {
            config.setWithdrawalMaximumAmount(request.getWithdrawalMaximumAmount());
        }
        if (request.getWithdrawalDailyLimit() != null) {
            config.setWithdrawalDailyLimit(request.getWithdrawalDailyLimit());
        }
        // Legacy fields for backward compatibility
        if (request.getMonthlyPool() != null) {
            config.setMonthlyPool(request.getMonthlyPool());
        }
        if (request.getMaxReferralBonus() != null) {
            config.setMaxReferralBonus(request.getMaxReferralBonus());
        }
        if (request.getReferralBonusPerActive() != null) {
            config.setReferralBonusPerActive(request.getReferralBonusPerActive());
        }
        
        // Save to database
        SystemConfig savedConfig = systemConfigRepository.save(config);
        log.info("System configuration saved successfully");
        
        // Return updated config (reuse getSystemConfig to include tiers and levels)
        return getSystemConfig();
    }
}

