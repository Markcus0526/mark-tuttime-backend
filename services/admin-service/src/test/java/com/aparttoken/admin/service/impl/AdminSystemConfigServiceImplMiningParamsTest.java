package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.SystemConfigResponse;
import com.tuttime.admin.dto.UpdateSystemConfigRequest;
import com.tuttime.admin.repository.StatusLevelRepository;
import com.tuttime.admin.repository.StakingTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSystemConfigServiceImpl - Mining Params Tests")
class AdminSystemConfigServiceImplMiningParamsTest {

    @Mock
    private StakingTierRepository stakingTierRepository;

    @Mock
    private StatusLevelRepository statusLevelRepository;

    @InjectMocks
    private AdminSystemConfigServiceImpl systemConfigService;

    @BeforeEach
    void setUp() {
        when(stakingTierRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(Collections.emptyList());
        when(statusLevelRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(Collections.emptyList());
    }

    @Test
    @DisplayName("Should get system config with correct default mining parameters")
    void testGetSystemConfig_DefaultMiningParams_ReturnsCorrectValues() {
        // When
        SystemConfigResponse result = systemConfigService.getSystemConfig();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBaseMiningSpeed()).isEqualByComparingTo(new BigDecimal("0.3"));
        assertThat(result.getMaxMiningSpeed()).isEqualByComparingTo(new BigDecimal("27.777"));
        assertThat(result.getMinMiningIntervalMinutes()).isEqualTo(1);
        assertThat(result.getMaxMiningIntervalMinutes()).isEqualTo(5);
        assertThat(result.getMonthlyPoolAmount()).isEqualByComparingTo(new BigDecimal("50000000000"));
    }

    @Test
    @DisplayName("Should update system config with new mining parameters")
    void testUpdateSystemConfig_WithMiningParams_ReturnsUpdatedValues() {
        // Given
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                .baseMiningSpeed(new BigDecimal("0.5"))
                .maxMiningSpeed(new BigDecimal("30"))
                .minMiningIntervalMinutes(2)
                .maxMiningIntervalMinutes(10)
                .monthlyPoolAmount(new BigDecimal("60000000000"))
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBaseMiningSpeed()).isEqualByComparingTo(new BigDecimal("0.5"));
        assertThat(result.getMaxMiningSpeed()).isEqualByComparingTo(new BigDecimal("30"));
        assertThat(result.getMinMiningIntervalMinutes()).isEqualTo(2);
        assertThat(result.getMaxMiningIntervalMinutes()).isEqualTo(10);
        assertThat(result.getMonthlyPoolAmount()).isEqualByComparingTo(new BigDecimal("60000000000"));
    }

    @Test
    @DisplayName("Should use defaults when update request has null values")
    void testUpdateSystemConfig_NullValues_UsesDefaults() {
        // Given
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                .baseMiningSpeed(null)
                .maxMiningSpeed(null)
                .minMiningIntervalMinutes(null)
                .maxMiningIntervalMinutes(null)
                .monthlyPoolAmount(null)
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBaseMiningSpeed()).isEqualByComparingTo(new BigDecimal("0.3"));
        assertThat(result.getMaxMiningSpeed()).isEqualByComparingTo(new BigDecimal("27.777"));
        assertThat(result.getMinMiningIntervalMinutes()).isEqualTo(1);
        assertThat(result.getMaxMiningIntervalMinutes()).isEqualTo(5);
        assertThat(result.getMonthlyPoolAmount()).isEqualByComparingTo(new BigDecimal("50000000000"));
    }

    @Test
    @DisplayName("Should update only provided mining parameters")
    void testUpdateSystemConfig_PartialUpdate_UpdatesOnlyProvided() {
        // Given
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                .baseMiningSpeed(new BigDecimal("0.4"))
                .maxMiningSpeed(null) // Not provided
                .minMiningIntervalMinutes(3)
                .maxMiningIntervalMinutes(null) // Not provided
                .monthlyPoolAmount(null) // Not provided
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBaseMiningSpeed()).isEqualByComparingTo(new BigDecimal("0.4"));
        assertThat(result.getMaxMiningSpeed()).isEqualByComparingTo(new BigDecimal("27.777")); // Default
        assertThat(result.getMinMiningIntervalMinutes()).isEqualTo(3);
        assertThat(result.getMaxMiningIntervalMinutes()).isEqualTo(5); // Default
        assertThat(result.getMonthlyPoolAmount()).isEqualByComparingTo(new BigDecimal("50000000000")); // Default
    }

    @Test
    @DisplayName("Should maintain backward compatibility with legacy fields")
    @SuppressWarnings("deprecation")
    void testGetSystemConfig_LegacyFields_StillPresent() {
        // When
        SystemConfigResponse result = systemConfigService.getSystemConfig();

        // Then
        assertThat(result).isNotNull();
        // Legacy fields should still be present for backward compatibility
        assertThat(result.getMonthlyPool()).isNotNull();
        assertThat(result.getMaxReferralBonus()).isNotNull();
        assertThat(result.getReferralBonusPerActive()).isNotNull();
    }

    @Test
    @DisplayName("Should handle large monthly pool amount correctly")
    void testUpdateSystemConfig_LargeMonthlyPool_HandlesCorrectly() {
        // Given
        BigDecimal largeAmount = new BigDecimal("100000000000"); // 100B
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                .monthlyPoolAmount(largeAmount)
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMonthlyPoolAmount()).isEqualByComparingTo(largeAmount);
    }

    @Test
    @DisplayName("Should validate mining interval range")
    void testUpdateSystemConfig_MiningIntervalRange_ValidatesCorrectly() {
        // Given
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                .minMiningIntervalMinutes(1)
                .maxMiningIntervalMinutes(5)
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getMinMiningIntervalMinutes()).isEqualTo(1);
        assertThat(result.getMaxMiningIntervalMinutes()).isEqualTo(5);
        // Note: Actual validation should be done in the controller/service layer
        // This test just verifies the values are set correctly
    }

    @Test
    @DisplayName("Should include staking tiers and status levels in config")
    void testGetSystemConfig_IncludesTiersAndLevels_ReturnsCompleteConfig() {
        // When
        SystemConfigResponse result = systemConfigService.getSystemConfig();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getStakingTiers()).isNotNull();
        assertThat(result.getStatusLevels()).isNotNull();
        verify(stakingTierRepository).findByIsActiveTrueOrderByDisplayOrderAsc();
        verify(statusLevelRepository).findByIsActiveTrueOrderByDisplayOrderAsc();
    }
}

