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
@DisplayName("AdminSystemConfigServiceImpl - Rewards Boost Params Tests")
class AdminSystemConfigServiceImplRewardsBoostTest {

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
    @DisplayName("Should get system config with correct default rewards boost parameters")
    void testGetSystemConfig_DefaultRewardsBoostParams_ReturnsCorrectValues() {
        // When
        SystemConfigResponse result = systemConfigService.getSystemConfig();

        // Then
        assertThat(result).isNotNull();
        // Note: referralBoostPerInvitation moved to MiningFormulaConfig
        assertThat(result.getMaxReferralBoosts()).isEqualTo(10);
        assertThat(result.getBookingBoostBase()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(result.getBookingBoostDurationDays()).isEqualTo(15);
        assertThat(result.getRentalBoostMultiplier()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(result.getRentalBoostDurationDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should update system config with new rewards boost parameters")
    void testUpdateSystemConfig_WithRewardsBoostParams_ReturnsUpdatedValues() {
        // Given
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                // Note: referralBoostPerInvitation moved to MiningFormulaConfig
                .maxReferralBoosts(15)
                .bookingBoostBase(new BigDecimal("3"))
                .bookingBoostDurationDays(20)
                .rentalBoostMultiplier(new BigDecimal("2.5"))
                .rentalBoostDurationDays(3)
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        // Note: referralBoostPerInvitation moved to MiningFormulaConfig
        assertThat(result.getMaxReferralBoosts()).isEqualTo(15);
        assertThat(result.getBookingBoostBase()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(result.getBookingBoostDurationDays()).isEqualTo(20);
        assertThat(result.getRentalBoostMultiplier()).isEqualByComparingTo(new BigDecimal("2.5"));
        assertThat(result.getRentalBoostDurationDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should use defaults when update request has null rewards boost values")
    void testUpdateSystemConfig_NullRewardsBoostValues_UsesDefaults() {
        // Given
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                // Note: referralBoostPerInvitation moved to MiningFormulaConfig
                .maxReferralBoosts(null)
                .bookingBoostBase(null)
                .bookingBoostDurationDays(null)
                .rentalBoostMultiplier(null)
                .rentalBoostDurationDays(null)
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        // Note: referralBoostPerInvitation moved to MiningFormulaConfig
        assertThat(result.getMaxReferralBoosts()).isEqualTo(10);
        assertThat(result.getBookingBoostBase()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(result.getBookingBoostDurationDays()).isEqualTo(15);
        assertThat(result.getRentalBoostMultiplier()).isEqualByComparingTo(new BigDecimal("2"));
        assertThat(result.getRentalBoostDurationDays()).isEqualTo(2);
    }

    @Test
    @DisplayName("Should update only provided rewards boost parameters")
    void testUpdateSystemConfig_PartialRewardsBoostUpdate_UpdatesOnlyProvided() {
        // Given
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                // Note: referralBoostPerInvitation moved to MiningFormulaConfig
                .maxReferralBoosts(null) // Not provided
                .bookingBoostBase(null) // Not provided
                .bookingBoostDurationDays(20)
                .rentalBoostMultiplier(null) // Not provided
                .rentalBoostDurationDays(null) // Not provided
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        // Note: referralBoostPerInvitation moved to MiningFormulaConfig
        assertThat(result.getMaxReferralBoosts()).isEqualTo(10); // Default
        assertThat(result.getBookingBoostBase()).isEqualByComparingTo(new BigDecimal("2")); // Default
        assertThat(result.getBookingBoostDurationDays()).isEqualTo(20);
        assertThat(result.getRentalBoostMultiplier()).isEqualByComparingTo(new BigDecimal("2")); // Default
        assertThat(result.getRentalBoostDurationDays()).isEqualTo(2); // Default
    }

    @Test
    @DisplayName("Should handle zero values for rewards boost parameters")
    void testUpdateSystemConfig_ZeroValues_HandlesCorrectly() {
        // Given
        UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
                // Note: referralBoostPerInvitation moved to MiningFormulaConfig
                .maxReferralBoosts(0)
                .bookingBoostBase(BigDecimal.ZERO)
                .bookingBoostDurationDays(0)
                .rentalBoostMultiplier(BigDecimal.ZERO)
                .rentalBoostDurationDays(0)
                .build();

        // When
        SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

        // Then
        assertThat(result).isNotNull();
        // Note: referralBoostPerInvitation moved to MiningFormulaConfig
        assertThat(result.getMaxReferralBoosts()).isEqualTo(0);
        assertThat(result.getBookingBoostBase()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getBookingBoostDurationDays()).isEqualTo(0);
        assertThat(result.getRentalBoostMultiplier()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getRentalBoostDurationDays()).isEqualTo(0);
    }
}

