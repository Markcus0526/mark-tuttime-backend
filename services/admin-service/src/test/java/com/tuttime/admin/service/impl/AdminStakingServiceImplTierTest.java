package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.ReorderTiersRequest;
import com.tuttime.admin.dto.StakingTierRequest;
import com.tuttime.admin.dto.StakingPoolResponse;
import com.tuttime.admin.entity.StakingTier;
import com.tuttime.admin.repository.StakingTierRepository;
import com.tuttime.admin.rest.client.StakingServiceRestClient;
import com.tuttime.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStakingServiceImpl - Tier Management Tests")
class AdminStakingServiceImplTierTest {

    @Mock
    private StakingTierRepository stakingTierRepository;
    
    @Mock
    private StakingServiceRestClient stakingServiceRestClient;

    @InjectMocks
    private AdminStakingServiceImpl stakingService;

    private StakingTier testTier;
    private StakingTierRequest testTierRequest;

    @BeforeEach
    void setUp() {
        testTier = StakingTier.builder()
                .tierId(1L)
                .tierName("Bronze")
                .minStakeAmount(new BigDecimal("1000"))
                .apBoostPerHour(new BigDecimal("10"))
                .displayOrder(0)
                .isActive(true)
                .description("1k APART -> +10 AP/hr")
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
                .createdBy(1L)
                .build();

        testTierRequest = StakingTierRequest.builder()
                .tierName("Silver")
                .minStakeAmount(new BigDecimal("10000"))
                .apBoostPerHour(new BigDecimal("20"))
                .displayOrder(1)
                .isActive(true)
                .description("10k APART -> +20 AP/hr")
                .build();
    }

    // Note: getAllTiers() method removed - use listStakingPools() instead
    // This test is kept for reference but should be updated to test listStakingPools() if needed

    @Test
    @DisplayName("Should get tier by ID successfully")
    void testGetTierById_Success_ReturnsTier() {
        // Given
        Long poolId = 1L;
        java.util.Map<String, Object> poolMap = new java.util.HashMap<>();
        poolMap.put("poolId", poolId);
        poolMap.put("poolName", "Bronze");
        poolMap.put("minStakeAmount", new BigDecimal("1000"));
        poolMap.put("boostMultiplier", new BigDecimal("10"));
        poolMap.put("isActive", true);
        
        when(stakingServiceRestClient.getPoolById(poolId)).thenReturn(poolMap);

        // When
        StakingPoolResponse result = stakingService.getTierById(poolId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoolId()).isEqualTo(poolId);
        assertThat(result.getPoolName()).isEqualTo("Bronze");
        assertThat(result.getMinStakeAmount()).isEqualByComparingTo(new BigDecimal("1000"));
        assertThat(result.getBoostMultiplier()).isEqualByComparingTo(new BigDecimal("10"));
        verify(stakingServiceRestClient).getPoolById(poolId);
    }

    @Test
    @DisplayName("Should throw exception when tier not found")
    void testGetTierById_NotFound_ThrowsException() {
        // Given
        Long poolId = 999L;
        when(stakingServiceRestClient.getPoolById(poolId))
                .thenThrow(new RuntimeException("Pool not found"));

        // When/Then
        assertThatThrownBy(() -> stakingService.getTierById(poolId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Staking pool not found");
    }

    @Test
    @DisplayName("Should create tier successfully")
    void testCreateTier_Success_CreatesTier() {
        // Given
        testTierRequest.setPoolName("Silver");
        testTierRequest.setLockPeriodDays(30);
        testTierRequest.setApyPercentage(new BigDecimal("5.0"));
        testTierRequest.setBoostMultiplier(new BigDecimal("20"));
        
        java.util.Map<String, Object> poolMap = new java.util.HashMap<>();
        poolMap.put("poolId", 2L);
        poolMap.put("poolName", "Silver");
        poolMap.put("minStakeAmount", new BigDecimal("10000"));
        poolMap.put("boostMultiplier", new BigDecimal("20"));
        poolMap.put("isActive", true);
        poolMap.put("createdAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        poolMap.put("updatedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        
        when(stakingServiceRestClient.createPool(any(java.util.Map.class))).thenReturn(poolMap);

        // When
        StakingPoolResponse result = stakingService.createTier(testTierRequest, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoolName()).isEqualTo("Silver");
        assertThat(result.getMinStakeAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(result.getBoostMultiplier()).isEqualByComparingTo(new BigDecimal("20"));
        verify(stakingServiceRestClient).createPool(any(java.util.Map.class));
    }

    @Test
    @DisplayName("Should throw exception when required fields are missing")
    void testCreateTier_MissingFields_ThrowsException() {
        // Given
        StakingTierRequest invalidRequest = StakingTierRequest.builder()
                .tierName("Test")
                .build(); // Missing required fields

        // When/Then
        assertThatThrownBy(() -> stakingService.createTier(invalidRequest, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("required");
    }

    @Test
    @DisplayName("Should update tier successfully")
    void testUpdateTier_Success_UpdatesTier() {
        // Given
        Long poolId = 1L;
        StakingTierRequest updateRequest = StakingTierRequest.builder()
                .poolName("Bronze Updated")
                .boostMultiplier(new BigDecimal("15"))
                .description("Updated description")
                .build();
        
        java.util.Map<String, Object> poolMap = new java.util.HashMap<>();
        poolMap.put("poolId", poolId);
        poolMap.put("poolName", "Bronze Updated");
        poolMap.put("boostMultiplier", new BigDecimal("15"));
        poolMap.put("description", "Updated description");
        poolMap.put("updatedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        
        when(stakingServiceRestClient.updatePool(eq(poolId), any(java.util.Map.class))).thenReturn(poolMap);

        // When
        StakingPoolResponse result = stakingService.updateTier(poolId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getPoolName()).isEqualTo("Bronze Updated");
        assertThat(result.getBoostMultiplier()).isEqualByComparingTo(new BigDecimal("15"));
        verify(stakingServiceRestClient).updatePool(eq(poolId), any(java.util.Map.class));
    }

    @Test
    @DisplayName("Should update tier with min stake amount")
    void testUpdateTier_WithMinStake_Success() {
        // Given
        Long poolId = 1L;
        StakingTierRequest updateRequest = StakingTierRequest.builder()
                .minStakeAmount(new BigDecimal("10000"))
                .build();
        
        java.util.Map<String, Object> poolMap = new java.util.HashMap<>();
        poolMap.put("poolId", poolId);
        poolMap.put("minStakeAmount", new BigDecimal("10000"));
        
        when(stakingServiceRestClient.updatePool(eq(poolId), any(java.util.Map.class))).thenReturn(poolMap);

        // When
        StakingPoolResponse result = stakingService.updateTier(poolId, updateRequest);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMinStakeAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        verify(stakingServiceRestClient).updatePool(eq(poolId), any(java.util.Map.class));
    }

    @Test
    @DisplayName("Should delete tier successfully")
    void testDeleteTier_Success_DeletesTier() {
        // Given
        Long poolId = 1L;
        doNothing().when(stakingServiceRestClient).deletePool(poolId);

        // When
        stakingService.deleteTier(poolId);

        // Then
        verify(stakingServiceRestClient).deletePool(poolId);
    }

    @Test
    @DisplayName("Should throw exception when deleting non-existent tier")
    void testDeleteTier_NotFound_ThrowsException() {
        // Given
        Long poolId = 999L;
        doThrow(new RuntimeException("Pool not found"))
                .when(stakingServiceRestClient).deletePool(poolId);

        // When/Then
        assertThatThrownBy(() -> stakingService.deleteTier(poolId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to delete");
    }

    @Test
    @DisplayName("Should throw exception when reordering (not supported)")
    void testReorderTiers_NotSupported_ThrowsException() {
        // Given
        ReorderTiersRequest request = ReorderTiersRequest.builder()
                .tierIds(Arrays.asList(3L, 1L, 2L))
                .build();

        // When/Then
        assertThatThrownBy(() -> stakingService.reorderTiers(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not applicable");
    }

    @Test
    @DisplayName("Should throw exception when reordering with empty list (not supported)")
    void testReorderTiers_EmptyList_ThrowsException() {
        // Given
        ReorderTiersRequest request = ReorderTiersRequest.builder()
                .tierIds(Collections.emptyList())
                .build();

        // When/Then
        assertThatThrownBy(() -> stakingService.reorderTiers(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not applicable");
    }

    @Test
    @DisplayName("Should create tier with default values")
    void testCreateTier_WithDefaults_UsesDefaults() {
        // Given
        StakingTierRequest request = StakingTierRequest.builder()
                .poolName("Gold")
                .minStakeAmount(new BigDecimal("50000"))
                .boostMultiplier(new BigDecimal("35"))
                .lockPeriodDays(30)
                .apyPercentage(new BigDecimal("5.0"))
                .build(); // isActive will default to true
        
        java.util.Map<String, Object> poolMap = new java.util.HashMap<>();
        poolMap.put("poolId", 3L);
        poolMap.put("poolName", "Gold");
        poolMap.put("minStakeAmount", new BigDecimal("50000"));
        poolMap.put("boostMultiplier", new BigDecimal("35"));
        poolMap.put("isActive", true);
        poolMap.put("createdAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        poolMap.put("updatedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
        
        when(stakingServiceRestClient.createPool(any(java.util.Map.class))).thenReturn(poolMap);

        // When
        StakingPoolResponse result = stakingService.createTier(request, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getIsActive()).isTrue();
        verify(stakingServiceRestClient).createPool(argThat(map -> 
                Boolean.TRUE.equals(map.get("isActive"))
        ));
    }
}

