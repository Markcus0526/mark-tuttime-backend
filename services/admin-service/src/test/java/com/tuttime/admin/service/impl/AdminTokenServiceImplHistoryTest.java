package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.GlobalTransactionResponse;
import com.tuttime.admin.dto.StakingClaimResponse;
import com.tuttime.admin.grpc.client.TokenServiceGrpcClient;
import com.tuttime.admin.grpc.client.UserServiceGrpcClient;
import com.tuttime.common.proto.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminTokenServiceImpl History Tests")
class AdminTokenServiceImplHistoryTest {

	@Mock
	private TokenServiceGrpcClient tokenServiceGrpcClient;

	@Mock
	private UserServiceGrpcClient userServiceGrpcClient;

	@InjectMocks
	private AdminTokenServiceImpl adminTokenService;

	@Test
	@DisplayName("Should get deposit history successfully")
	void testGetDepositHistory_Success_ReturnsTransactions() {
		// Given
		String status = "CONFIRMED";
		String network = "Ethereum";
		OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
		OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC);
		PageRequest pageable = PageRequest.of(0, 10);

		GlobalTransaction tx = GlobalTransaction.newBuilder()
				.setOrderRequestId(1L)
				.setUserId(100L)
				.setAmount("1000.50")
				.setWalletAddress("0x123")
				.setTxHash("0xabc")
				.setProcessedState("CONFIRMED")
				.setNetwork("Ethereum")
				.setTransactionType("DEPOSIT")
				.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
				.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
				.setProcessedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
				.build();

		GetDepositHistoryResponse grpcResponse = GetDepositHistoryResponse.newBuilder()
				.addTransactions(tx)
				.setTotalPages(1)
				.setTotalElements(1)
				.setHasNext(false)
				.build();

		UserResponse user = UserResponse.newBuilder()
				.setUserId(100L)
				.setUsername("John Doe")
				.build();

		when(tokenServiceGrpcClient.getDepositHistory(
				eq(status), eq(network), anyString(), anyString(), eq(0), eq(10)))
				.thenReturn(grpcResponse);
		when(userServiceGrpcClient.getUserById(100L)).thenReturn(user);

		// When
		Page<GlobalTransactionResponse> result = adminTokenService.getDepositHistory(
				status, network, startDate, endDate, pageable);

		// Then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).hasSize(1);

		GlobalTransactionResponse dto = result.getContent().get(0);
		assertThat(dto.getOrderRequestId()).isEqualTo(1L);
		assertThat(dto.getUserId()).isEqualTo(100L);
		assertThat(dto.getUsername()).isEqualTo("John Doe");
		assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("1000.50"));
		assertThat(dto.getNetwork()).isEqualTo("Ethereum");
		assertThat(dto.getTransactionType()).isEqualTo("DEPOSIT");
	}

	@Test
	@DisplayName("Should get withdrawal history successfully")
	void testGetWithdrawalHistory_Success_ReturnsTransactions() {
		// Given
		String status = "COMPLETED";
		String network = "BSC";
		PageRequest pageable = PageRequest.of(0, 10);

		GlobalTransaction tx = GlobalTransaction.newBuilder()
				.setOrderRequestId(2L)
				.setUserId(200L)
				.setAmount("500.25")
				.setWalletAddress("0x456")
				.setTxHash("0xdef")
				.setProcessedState("COMPLETED")
				.setNetwork("BSC")
				.setTransactionType("WITHDRAWAL")
				.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
				.build();

		GetWithdrawalHistoryResponse grpcResponse = GetWithdrawalHistoryResponse.newBuilder()
				.addTransactions(tx)
				.setTotalPages(1)
				.setTotalElements(1)
				.setHasNext(false)
				.build();

		UserResponse user = UserResponse.newBuilder()
				.setUserId(200L)
				.setUsername("Jane Smith")
				.build();

		when(tokenServiceGrpcClient.getWithdrawalHistory(
				eq(status), eq(network), isNull(), isNull(), eq(0), eq(10)))
				.thenReturn(grpcResponse);
		when(userServiceGrpcClient.getUserById(200L)).thenReturn(user);

		// When
		Page<GlobalTransactionResponse> result = adminTokenService.getWithdrawalHistory(
				status, network, null, null, pageable);

		// Then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getTransactionType()).isEqualTo("WITHDRAWAL");
	}

	@Test
	@DisplayName("Should get burn history successfully")
	void testGetBurnHistory_Success_ReturnsTransactions() {
		// Given
		PageRequest pageable = PageRequest.of(0, 10);

		GlobalTransaction tx = GlobalTransaction.newBuilder()
				.setOrderRequestId(3L)
				.setUserId(300L)
				.setAmount("50.75")
				.setProcessedState("CONFIRMED")
				.setTransactionType("BURN")
				.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
				.build();

		GetBurnHistoryResponse grpcResponse = GetBurnHistoryResponse.newBuilder()
				.addTransactions(tx)
				.setTotalPages(1)
				.setTotalElements(1)
				.setHasNext(false)
				.build();

		UserResponse user = UserResponse.newBuilder()
				.setUserId(300L)
				.setUsername("Bob Johnson")
				.build();

		when(tokenServiceGrpcClient.getBurnHistory(isNull(), isNull(), eq(0), eq(10)))
				.thenReturn(grpcResponse);
		when(userServiceGrpcClient.getUserById(300L)).thenReturn(user);

		// When
		Page<GlobalTransactionResponse> result = adminTokenService.getBurnHistory(null, null, pageable);

		// Then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getTransactionType()).isEqualTo("BURN");
	}

	@Test
	@DisplayName("Should get claim history successfully")
	void testGetClaimHistory_Success_ReturnsClaims() {
		// Given
		Long userId = 400L;
		PageRequest pageable = PageRequest.of(0, 10);

		StakingClaim claim = StakingClaim.newBuilder()
				.setTransactionId(1L)
				.setUserId(400L)
				.setStakingId(10L)
				.setPoolId(1L)
				.setPoolName("Premium Pool")
				.setClaimedAmount("100.50")
				.setStatus("CONFIRMED")
				.setClaimedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
				.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
				.build();

		GetClaimHistoryResponse grpcResponse = GetClaimHistoryResponse.newBuilder()
				.addClaims(claim)
				.setTotalPages(1)
				.setTotalElements(1)
				.setHasNext(false)
				.build();

		UserResponse user = UserResponse.newBuilder()
				.setUserId(400L)
				.setUsername("Alice Brown")
				.build();

		when(tokenServiceGrpcClient.getClaimHistory(eq(userId), isNull(), isNull(), eq(0), eq(10)))
				.thenReturn(grpcResponse);
		when(userServiceGrpcClient.getUserById(400L)).thenReturn(user);

		// When
		Page<StakingClaimResponse> result = adminTokenService.getClaimHistory(userId, null, null, pageable);

		// Then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).hasSize(1);

		StakingClaimResponse dto = result.getContent().get(0);
		assertThat(dto.getTransactionId()).isEqualTo(1L);
		assertThat(dto.getUserId()).isEqualTo(400L);
		assertThat(dto.getUsername()).isEqualTo("Alice Brown");
		assertThat(dto.getClaimedAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
		assertThat(dto.getPoolName()).isEqualTo("Premium Pool");
	}

	@Test
	@DisplayName("Should handle null userId in burn history")
	void testGetBurnHistory_NullUserId_HandlesGracefully() {
		// Given
		PageRequest pageable = PageRequest.of(0, 10);

		GlobalTransaction tx = GlobalTransaction.newBuilder()
				.setOrderRequestId(4L)
				.setUserId(0) // System burn, no user
				.setAmount("25.00")
				.setProcessedState("CONFIRMED")
				.setTransactionType("BURN")
				.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC).toString())
				.build();

		GetBurnHistoryResponse grpcResponse = GetBurnHistoryResponse.newBuilder()
				.addTransactions(tx)
				.setTotalPages(1)
				.setTotalElements(1)
				.setHasNext(false)
				.build();

		when(tokenServiceGrpcClient.getBurnHistory(isNull(), isNull(), eq(0), eq(10)))
				.thenReturn(grpcResponse);

		// When
		Page<GlobalTransactionResponse> result = adminTokenService.getBurnHistory(null, null, pageable);

		// Then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent().get(0).getUserId()).isNull();
	}
}

