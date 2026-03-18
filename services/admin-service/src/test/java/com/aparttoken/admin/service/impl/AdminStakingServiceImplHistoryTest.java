package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.StakingHistoryResponse;
import com.tuttime.admin.grpc.client.StakingServiceGrpcClient;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminStakingServiceImpl History Tests")
class AdminStakingServiceImplHistoryTest {

    @Mock
    private StakingServiceGrpcClient stakingServiceGrpcClient;

    @Mock
    private UserServiceGrpcClient userServiceGrpcClient;

    @InjectMocks
    private AdminStakingServiceImpl adminStakingService;

    @Test
    @DisplayName("Should get staking history successfully")
    void testGetStakingHistory_Success_ReturnsHistories() {
        // Given
        Long poolId = 1L;
        Long userId = 100L;
        PageRequest pageable = PageRequest.of(0, 10);

        StakingHistory history = StakingHistory.newBuilder()
                .setStakingId(1L)
                .setUserId(100L)
                .setPoolId(1L)
                .setPoolName("Premium Pool")
                .setAmount("5000.50")
                .setStatus("ACTIVE")
                .setStakedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(10).toString())
                .setUnlockAt(OffsetDateTime.now(ZoneOffset.UTC).plusDays(20).toString())
                .setOperationType("STAKE")
                .build();

        GetStakingHistoryResponse grpcResponse = GetStakingHistoryResponse.newBuilder()
                .addHistories(history)
                .setTotalPages(1)
                .setTotalElements(1)
                .setHasNext(false)
                .build();

        UserResponse user = UserResponse.newBuilder()
                .setUserId(100L)
                .setUsername("John Doe")
                .build();

        when(stakingServiceGrpcClient.getStakingHistory(
                eq(poolId), eq(userId), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(grpcResponse);
        when(userServiceGrpcClient.getUserById(100L)).thenReturn(user);

        // When
        Page<StakingHistoryResponse> result = adminStakingService.getStakingHistory(
                poolId, userId, null, null, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
        
        StakingHistoryResponse dto = result.getContent().get(0);
        assertThat(dto.getStakingId()).isEqualTo(1L);
        assertThat(dto.getUserId()).isEqualTo(100L);
        assertThat(dto.getUsername()).isEqualTo("John Doe");
        assertThat(dto.getPoolId()).isEqualTo(1L);
        assertThat(dto.getPoolName()).isEqualTo("Premium Pool");
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("5000.50"));
        assertThat(dto.getOperationType()).isEqualTo("STAKE");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("Should get unstaking history successfully")
    void testGetUnstakingHistory_Success_ReturnsHistories() {
        // Given
        Long poolId = 2L;
        Long userId = 200L;
        OffsetDateTime startDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(30);
        OffsetDateTime endDate = OffsetDateTime.now(ZoneOffset.UTC);
        PageRequest pageable = PageRequest.of(0, 10);

        StakingHistory history = StakingHistory.newBuilder()
                .setStakingId(2L)
                .setUserId(200L)
                .setPoolId(2L)
                .setPoolName("Standard Pool")
                .setAmount("3000.25")
                .setStatus("UNLOCKING")
                .setStakedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(30).toString())
                .setUnstakeRequestedAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(5).toString())
                .setUnlockAt(OffsetDateTime.now(ZoneOffset.UTC).minusDays(5).toString())
                .setOperationType("UNSTAKE")
                .build();

        GetUnstakingHistoryResponse grpcResponse = GetUnstakingHistoryResponse.newBuilder()
                .addHistories(history)
                .setTotalPages(1)
                .setTotalElements(1)
                .setHasNext(false)
                .build();

        UserResponse user = UserResponse.newBuilder()
                .setUserId(200L)
                .setUsername("Jane Smith")
                .build();

        when(stakingServiceGrpcClient.getUnstakingHistory(
                eq(poolId), eq(userId), anyString(), anyString(), eq(0), eq(10)))
                .thenReturn(grpcResponse);
        when(userServiceGrpcClient.getUserById(200L)).thenReturn(user);

        // When
        Page<StakingHistoryResponse> result = adminStakingService.getUnstakingHistory(
                poolId, userId, startDate, endDate, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getOperationType()).isEqualTo("UNSTAKE");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo("UNLOCKING");
    }

    @Test
    @DisplayName("Should handle empty staking history")
    void testGetStakingHistory_Empty_ReturnsEmpty() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        GetStakingHistoryResponse grpcResponse = GetStakingHistoryResponse.newBuilder()
                .setTotalPages(0)
                .setTotalElements(0)
                .setHasNext(false)
                .build();

        when(stakingServiceGrpcClient.getStakingHistory(
                isNull(), isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(grpcResponse);

        // When
        Page<StakingHistoryResponse> result = adminStakingService.getStakingHistory(
                null, null, null, null, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("Should handle missing username gracefully")
    void testGetStakingHistory_MissingUsername_HandlesGracefully() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);

        StakingHistory history = StakingHistory.newBuilder()
                .setStakingId(1L)
                .setUserId(100L)
                .setPoolId(1L)
                .setPoolName("Premium Pool")
                .setAmount("5000.50")
                .setStatus("ACTIVE")
                .setOperationType("STAKE")
                .build();

        GetStakingHistoryResponse grpcResponse = GetStakingHistoryResponse.newBuilder()
                .addHistories(history)
                .setTotalPages(1)
                .setTotalElements(1)
                .setHasNext(false)
                .build();

        when(stakingServiceGrpcClient.getStakingHistory(
                isNull(), isNull(), isNull(), isNull(), eq(0), eq(10)))
                .thenReturn(grpcResponse);
        when(userServiceGrpcClient.getUserById(100L))
                .thenThrow(new RuntimeException("User not found"));

        // When
        Page<StakingHistoryResponse> result = adminStakingService.getStakingHistory(
                null, null, null, null, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUsername()).isEmpty();
    }
}

