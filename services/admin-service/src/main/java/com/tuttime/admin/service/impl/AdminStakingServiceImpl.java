package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.ReorderTiersRequest;
import com.tuttime.admin.dto.StakingHistoryResponse;
import com.tuttime.admin.dto.StakingPositionsResponse;
import com.tuttime.admin.dto.StakingPoolResponse;
import com.tuttime.admin.dto.StakingTierRequest;
import com.tuttime.admin.dto.StakingTierResponse;
import com.tuttime.admin.entity.StakingTier;
import com.tuttime.admin.grpc.client.StakingServiceGrpcClient;
import com.tuttime.admin.grpc.client.UserServiceGrpcClient;
import com.tuttime.admin.repository.StakingTierRepository;
import com.tuttime.admin.rest.client.StakingServiceRestClient;
import com.tuttime.admin.service.AdminStakingService;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.proto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Staking Service Implementation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStakingServiceImpl implements AdminStakingService {

    private final StakingServiceGrpcClient stakingServiceGrpcClient;
    private final UserServiceGrpcClient userServiceGrpcClient;
    private final StakingTierRepository stakingTierRepository;
    private final StakingServiceRestClient stakingServiceRestClient;
    
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public StakingPositionsResponse getStakingPositions(Long userId) {
        // Staking-service not deployed - return empty positions immediately
        log.debug("Staking-service not deployed - returning empty positions for user: {}", userId);
        return StakingPositionsResponse.builder()
                .userId(userId)
                .positions(List.of())
                .build();
    }

    @Override
    public List<StakingPoolResponse> listStakingPools() {
        // Staking-service not deployed - return empty list immediately
        log.debug("Staking-service not deployed - returning empty pools list");
        return List.of();
    }
    
    /**
     * Map staking-service pool data to admin-service DTO
     * Maps all fields from staking_pools table structure
     */
    private StakingPoolResponse mapToStakingPoolResponse(Map<String, Object> poolMap) {
        log.debug("Mapping pool data: available keys = {}", poolMap.keySet());
        
        // Try primary field names first, fallback to legacy names if needed
        Long poolId = getLong(poolMap, "poolId");
        String poolName = getString(poolMap, "poolName");
        if (poolName == null) {
            poolName = getString(poolMap, "name"); // Fallback to legacy name
        }
        
        BigDecimal minStakeAmount = getBigDecimal(poolMap, "minStakeAmount");
        if (minStakeAmount == null) {
            minStakeAmount = getBigDecimal(poolMap, "minAmount"); // Fallback to legacy name
        }
        
        BigDecimal maxStakeAmount = getBigDecimal(poolMap, "maxStakeAmount");
        if (maxStakeAmount == null) {
            maxStakeAmount = getBigDecimal(poolMap, "maxAmount"); // Fallback to legacy name
        }
        
        BigDecimal boostMultiplier = getBigDecimal(poolMap, "boostMultiplier");
        String poolType = getString(poolMap, "poolType");
        Integer lockPeriodDays = getInteger(poolMap, "lockPeriodDays");
        BigDecimal apyPercentage = getBigDecimal(poolMap, "apyPercentage");
        Boolean isActive = getBoolean(poolMap, "isActive");
        String description = getString(poolMap, "description");
        
        // Parse datetime - handle OffsetDateTime objects and strings
        OffsetDateTime createdAt = getOffsetDateTime(poolMap, "createdAt");
        OffsetDateTime updatedAt = getOffsetDateTime(poolMap, "updatedAt");
        
        log.debug("Mapped values: poolId={}, poolName={}, poolType={}, minStakeAmount={}, apyPercentage={}, isActive={}", 
                poolId, poolName, poolType, minStakeAmount, apyPercentage, isActive);
        
        return StakingPoolResponse.builder()
                // Primary fields matching staking_pools table
                .poolId(poolId)
                .poolName(poolName)
                .poolType(poolType)
                .minStakeAmount(minStakeAmount)
                .maxStakeAmount(maxStakeAmount)
                .lockPeriodDays(lockPeriodDays)
                .apyPercentage(apyPercentage)
                .boostMultiplier(boostMultiplier)
                .isActive(isActive)
                .description(description)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                // Legacy fields are @JsonIgnore, so we don't set them
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
        return value != null ? value.toString() : null;
    }

    @Override
    public Page<StakingHistoryResponse> getStakingHistory(Long poolId, Long userId, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable) {
        // Staking-service not deployed - return empty page immediately
        log.debug("Staking-service not deployed - returning empty staking history");
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }

    @Override
    public Page<StakingHistoryResponse> getUnstakingHistory(Long poolId, Long userId, OffsetDateTime startDate, OffsetDateTime endDate, org.springframework.data.domain.Pageable pageable) {
        // Staking-service not deployed - return empty page immediately
        log.debug("Staking-service not deployed - returning empty unstaking history");
        return new PageImpl<>(
                List.of(),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()),
                0L
        );
    }

    /**
     * Parses a datetime string to OffsetDateTime.
     * 
     * <p><b>Timezone Note:</b> Parsed dates are converted to UTC timezone.
     * All API responses serialize dates in UTC, so this is safe.
     * 
     * @param dateTimeStr The datetime string to parse (ISO format, UTC)
     * @return Parsed OffsetDateTime in UTC timezone, or null if parsing fails
     */
    private OffsetDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            // Parse as OffsetDateTime and ensure UTC timezone
            return OffsetDateTime.parse(dateTimeStr).withOffsetSameInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            log.warn("Failed to parse datetime: {}", dateTimeStr, e);
            return null;
        }
    }

    // ============================================================================
    // Staking Tier Management
    // ============================================================================

    // Note: getAllTiers() removed - use listStakingPools() instead which returns pools from staking-service

    @Override
    @Transactional(readOnly = true)
    public StakingPoolResponse getTierById(Long poolId) {
        // Staking-service not deployed - throw exception
        log.warn("Staking-service not deployed - cannot get pool by ID: {}", poolId);
        throw new BusinessException("Staking service is not available");
    }

    @Override
    @Transactional
    @SuppressWarnings("deprecation") // Using deprecated methods for backward compatibility
    public StakingPoolResponse createTier(StakingTierRequest request, Long createdBy) {
        // Staking-service not deployed - throw exception
        log.warn("Staking-service not deployed - cannot create staking pool");
        throw new BusinessException("Staking service is not available");
    }

    @Override
    @Transactional
    @SuppressWarnings("deprecation") // Using deprecated methods for backward compatibility
    public StakingPoolResponse updateTier(Long tierId, StakingTierRequest request) {
        // Staking-service not deployed - throw exception
        log.warn("Staking-service not deployed - cannot update staking pool: poolId={}", tierId);
        throw new BusinessException("Staking service is not available");
    }

    @Override
    @Transactional
    public void deleteTier(Long tierId) {
        // Staking-service not deployed - throw exception
        log.warn("Staking-service not deployed - cannot delete staking pool: poolId={}", tierId);
        throw new BusinessException("Staking service is not available");
    }

    @Override
    @Transactional
    public void reorderTiers(ReorderTiersRequest request) {
        log.warn("Reordering staking pools is not supported - staking-service pools do not have display order");
        throw new BusinessException("Reordering is not applicable for staking-service pools. Pools are managed directly in staking-service and do not support display order.");
    }

    /**
     * Convert StakingTier entity to StakingTierResponse DTO
     */
    /**
     * Map staking-service pool response to StakingTierResponse
     * Maps all fields from staking_pools table structure
     */
    private StakingTierResponse mapPoolToTierResponse(Map<String, Object> poolMap) {
        Long poolId = getLong(poolMap, "poolId");
        String poolName = getString(poolMap, "poolName");
        BigDecimal boostMultiplier = getBigDecimal(poolMap, "boostMultiplier");
        
        // Parse datetime strings if they exist
        OffsetDateTime createdAt = null;
        OffsetDateTime updatedAt = null;
        Object createdAtObj = poolMap.get("createdAt");
        Object updatedAtObj = poolMap.get("updatedAt");
        
        if (createdAtObj != null) {
            if (createdAtObj instanceof OffsetDateTime) {
                createdAt = ((OffsetDateTime) createdAtObj).withOffsetSameInstant(ZoneOffset.UTC);
            } else if (createdAtObj instanceof String) {
                createdAt = parseDateTime((String) createdAtObj);
            }
        }
        
        if (updatedAtObj != null) {
            if (updatedAtObj instanceof OffsetDateTime) {
                updatedAt = ((OffsetDateTime) updatedAtObj).withOffsetSameInstant(ZoneOffset.UTC);
            } else if (updatedAtObj instanceof String) {
                updatedAt = parseDateTime((String) updatedAtObj);
            }
        }
        
        StakingTierResponse.StakingTierResponseBuilder builder = StakingTierResponse.builder()
                // Primary fields matching staking_pools table
                .poolId(poolId)
                .poolName(poolName)
                .poolType(getString(poolMap, "poolType"))
                .minStakeAmount(getBigDecimal(poolMap, "minStakeAmount"))
                .maxStakeAmount(getBigDecimal(poolMap, "maxStakeAmount"))
                .lockPeriodDays(getInteger(poolMap, "lockPeriodDays"))
                .apyPercentage(getBigDecimal(poolMap, "apyPercentage"))
                .boostMultiplier(boostMultiplier)
                .isActive(getBoolean(poolMap, "isActive"))
                .description(getString(poolMap, "description"))
                .createdAt(createdAt)
                .updatedAt(updatedAt);
        
        // Legacy fields for backward compatibility
        @SuppressWarnings("deprecation")
        StakingTierResponse response = builder
                .tierId(poolId)
                .tierName(poolName)
                .apBoostPerHour(boostMultiplier)
                .displayOrder(0) // Not in staking_pools table
                .createdBy(null) // Not in staking_pools table
                .build();
        
        return response;
    }

    /**
     * Helper method to safely extract Boolean from Map
     */
    private Boolean getBoolean(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return null;
    }

    @SuppressWarnings("deprecation") // Using deprecated builder methods for backward compatibility
    private StakingTierResponse convertToResponse(StakingTier tier) {
        StakingTierResponse.StakingTierResponseBuilder builder = StakingTierResponse.builder()
                .poolId(tier.getTierId())
                .poolName(tier.getTierName())
                .minStakeAmount(tier.getMinStakeAmount())
                .boostMultiplier(tier.getApBoostPerHour())
                .isActive(tier.getIsActive())
                .description(tier.getDescription())
                .createdAt(tier.getCreatedAt())
                .updatedAt(tier.getUpdatedAt());
        
        // Legacy fields for backward compatibility
        return builder
                .tierId(tier.getTierId())
                .tierName(tier.getTierName())
                .apBoostPerHour(tier.getApBoostPerHour())
                .displayOrder(tier.getDisplayOrder())
                .createdBy(tier.getCreatedBy())
                .build();
    }
}

