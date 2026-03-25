package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.InvestorRequest;
import com.tuttime.admin.dto.InvestorResponse;
import com.tuttime.admin.dto.VestingPoolRequest;
import com.tuttime.admin.dto.VestingPoolResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminVestingServiceImpl Tests")
class AdminVestingServiceImplTest {

	@InjectMocks
	private AdminVestingServiceImpl vestingService;

	@Test
	@DisplayName("Should list pools - returns empty page (placeholder)")
	void testListPools_Placeholder_ReturnsEmptyPage() {
		// Given
		PageRequest pageable = PageRequest.of(0, 10);

		// When
		Page<VestingPoolResponse> result = vestingService.listPools(pageable);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isEqualTo(0);
	}

	@Test
	@DisplayName("Should throw exception when getting pool (not implemented)")
	void testGetPool_NotImplemented_ThrowsException() {
		// Given
		Long poolId = 1L;

		// When/Then
		assertThatThrownBy(() -> vestingService.getPool(poolId))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Not implemented yet");
	}

	@Test
	@DisplayName("Should throw exception when creating pool (not implemented)")
	void testCreatePool_NotImplemented_ThrowsException() {
		// Given
		VestingPoolRequest request = VestingPoolRequest.builder()
				.poolName("Test Pool")
				.contractAddress("0x1234567890abcdef")
				.totalAmount(new BigDecimal("1000000"))
				.vestingSchedule("LINEAR")
				.build();

		// When/Then
		assertThatThrownBy(() -> vestingService.createPool(request))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Not implemented yet");
	}

	@Test
	@DisplayName("Should throw exception when updating pool (not implemented)")
	void testUpdatePool_NotImplemented_ThrowsException() {
		// Given
		Long poolId = 1L;
		VestingPoolRequest request = VestingPoolRequest.builder()
				.poolName("Updated Pool")
				.contractAddress("0x1234567890abcdef")
				.totalAmount(new BigDecimal("2000000"))
				.vestingSchedule("LINEAR")
				.build();

		// When/Then
		assertThatThrownBy(() -> vestingService.updatePool(poolId, request))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Not implemented yet");
	}

	@Test
	@DisplayName("Should throw exception when getting pool balance (not implemented)")
	void testGetPoolBalance_NotImplemented_ThrowsException() {
		// Given
		Long poolId = 1L;

		// When/Then
		assertThatThrownBy(() -> vestingService.getPoolBalance(poolId))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Not implemented yet");
	}

	@Test
	@DisplayName("Should throw exception when funding pool (not implemented)")
	void testFundPool_NotImplemented_ThrowsException() {
		// Given
		Long poolId = 1L;
		BigDecimal amount = new BigDecimal("100000");

		// When/Then
		assertThatThrownBy(() -> vestingService.fundPool(poolId, amount))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Not implemented yet");
	}

	@Test
	@DisplayName("Should list investors - returns empty page (placeholder)")
	void testListInvestors_Placeholder_ReturnsEmptyPage() {
		// Given
		Long poolId = 1L;
		PageRequest pageable = PageRequest.of(0, 10);

		// When
		Page<InvestorResponse> result = vestingService.listInvestors(poolId, pageable);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getContent()).isEmpty();
		assertThat(result.getTotalElements()).isEqualTo(0);
	}

	@Test
	@DisplayName("Should throw exception when adding investor (not implemented)")
	void testAddInvestor_NotImplemented_ThrowsException() {
		// Given
		Long poolId = 1L;
		InvestorRequest request = InvestorRequest.builder()
				.userId(100L)
				.walletAddress("0xabcdef1234567890")
				.allocation(new BigDecimal("50000"))
				.vestingStartDate(java.time.LocalDate.now())
				.build();

		// When/Then
		assertThatThrownBy(() -> vestingService.addInvestor(poolId, request))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Not implemented yet");
	}

	@Test
	@DisplayName("Should throw exception when getting claim status (not implemented)")
	void testGetInvestorClaimStatus_NotImplemented_ThrowsException() {
		// Given
		Long poolId = 1L;
		Long investorId = 1L;

		// When/Then
		assertThatThrownBy(() -> vestingService.getInvestorClaimStatus(poolId, investorId))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Not implemented yet");
	}
}

