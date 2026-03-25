package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.SystemConfigResponse;
import com.tuttime.admin.dto.UpdateSystemConfigRequest;
import com.tuttime.admin.repository.StakingTierRepository;
import com.tuttime.admin.repository.StatusLevelRepository;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSystemConfigServiceImpl - Extra Params Tests")
class AdminSystemConfigServiceImplExtraParamsTest {

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
	@DisplayName("Should get system config with correct default extra parameters")
	void testGetSystemConfig_DefaultExtraParams_ReturnsCorrectValues() {
		// When
		SystemConfigResponse result = systemConfigService.getSystemConfig();

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWithdrawalPlatformFeePercent()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.getDepositMinimumAmount()).isNull();
		assertThat(result.getWithdrawalMinimumAmount()).isNull();
		assertThat(result.getWithdrawalMaximumAmount()).isNull();
		assertThat(result.getWithdrawalDailyLimit()).isNull();
	}

	@Test
	@DisplayName("Should update system config with new extra parameters")
	void testUpdateSystemConfig_WithExtraParams_ReturnsUpdatedValues() {
		// Given
		UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
				.withdrawalPlatformFeePercent(new BigDecimal("0.5")) // 0.5%
				.depositMinimumAmount(new BigDecimal("100"))
				.withdrawalMinimumAmount(new BigDecimal("50"))
				.withdrawalMaximumAmount(new BigDecimal("10000"))
				.withdrawalDailyLimit(5)
				.build();

		// When
		SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWithdrawalPlatformFeePercent()).isEqualByComparingTo(new BigDecimal("0.5"));
		assertThat(result.getDepositMinimumAmount()).isEqualByComparingTo(new BigDecimal("100"));
		assertThat(result.getWithdrawalMinimumAmount()).isEqualByComparingTo(new BigDecimal("50"));
		assertThat(result.getWithdrawalMaximumAmount()).isEqualByComparingTo(new BigDecimal("10000"));
		assertThat(result.getWithdrawalDailyLimit()).isEqualTo(5);
	}

	@Test
	@DisplayName("Should use defaults when update request has null extra param values")
	void testUpdateSystemConfig_NullExtraParamValues_UsesDefaults() {
		// Given
		UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
				.withdrawalPlatformFeePercent(null)
				.depositMinimumAmount(null)
				.withdrawalMinimumAmount(null)
				.withdrawalMaximumAmount(null)
				.withdrawalDailyLimit(null)
				.build();

		// When
		SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWithdrawalPlatformFeePercent()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.getDepositMinimumAmount()).isNull();
		assertThat(result.getWithdrawalMinimumAmount()).isNull();
		assertThat(result.getWithdrawalMaximumAmount()).isNull();
		assertThat(result.getWithdrawalDailyLimit()).isNull();
	}

	@Test
	@DisplayName("Should update only provided extra parameters")
	void testUpdateSystemConfig_PartialExtraParamsUpdate_UpdatesOnlyProvided() {
		// Given
		UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
				.withdrawalPlatformFeePercent(new BigDecimal("1.0")) // 1%
				.depositMinimumAmount(null) // Not provided
				.withdrawalMinimumAmount(new BigDecimal("100"))
				.withdrawalMaximumAmount(null) // Not provided
				.withdrawalDailyLimit(10)
				.build();

		// When
		SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWithdrawalPlatformFeePercent()).isEqualByComparingTo(new BigDecimal("1.0"));
		assertThat(result.getDepositMinimumAmount()).isNull(); // Default
		assertThat(result.getWithdrawalMinimumAmount()).isEqualByComparingTo(new BigDecimal("100"));
		assertThat(result.getWithdrawalMaximumAmount()).isNull(); // Default
		assertThat(result.getWithdrawalDailyLimit()).isEqualTo(10);
	}

	@Test
	@DisplayName("Should handle platform fee percentage correctly")
	void testUpdateSystemConfig_PlatformFeePercentage_HandlesCorrectly() {
		// Given
		UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
				.withdrawalPlatformFeePercent(new BigDecimal("2.5")) // 2.5%
				.build();

		// When
		SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWithdrawalPlatformFeePercent()).isEqualByComparingTo(new BigDecimal("2.5"));
	}

	@Test
	@DisplayName("Should handle large withdrawal amounts correctly")
	void testUpdateSystemConfig_LargeWithdrawalAmounts_HandlesCorrectly() {
		// Given
		BigDecimal largeAmount = new BigDecimal("1000000"); // 1M
		UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
				.withdrawalMaximumAmount(largeAmount)
				.build();

		// When
		SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWithdrawalMaximumAmount()).isEqualByComparingTo(largeAmount);
	}

	@Test
	@DisplayName("Should handle zero platform fee correctly")
	void testUpdateSystemConfig_ZeroPlatformFee_HandlesCorrectly() {
		// Given
		UpdateSystemConfigRequest request = UpdateSystemConfigRequest.builder()
				.withdrawalPlatformFeePercent(BigDecimal.ZERO)
				.build();

		// When
		SystemConfigResponse result = systemConfigService.updateSystemConfig(request, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWithdrawalPlatformFeePercent()).isEqualByComparingTo(BigDecimal.ZERO);
	}

	@Test
	@DisplayName("Should handle setting limits to null (removing limits)")
	void testUpdateSystemConfig_RemoveLimits_HandlesCorrectly() {
		// Given - First set limits
		UpdateSystemConfigRequest setRequest = UpdateSystemConfigRequest.builder()
				.withdrawalMinimumAmount(new BigDecimal("100"))
				.withdrawalMaximumAmount(new BigDecimal("10000"))
				.withdrawalDailyLimit(5)
				.build();

		systemConfigService.updateSystemConfig(setRequest, 1L);

		// Then - Remove limits by setting to null
		UpdateSystemConfigRequest removeRequest = UpdateSystemConfigRequest.builder()
				.withdrawalMinimumAmount(null)
				.withdrawalMaximumAmount(null)
				.withdrawalDailyLimit(null)
				.build();

		// When
		SystemConfigResponse result = systemConfigService.updateSystemConfig(removeRequest, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getWithdrawalMinimumAmount()).isNull();
		assertThat(result.getWithdrawalMaximumAmount()).isNull();
		assertThat(result.getWithdrawalDailyLimit()).isNull();
	}
}

