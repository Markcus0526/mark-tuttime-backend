package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.ConversionLedgerEntry;
import com.tuttime.admin.grpc.client.ConversionServiceGrpcClient;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminConversionServiceImpl Tests")
class AdminConversionServiceImplTest {

    @Mock
    private ConversionServiceGrpcClient conversionServiceGrpcClient;

    @Mock
    private UserServiceGrpcClient userServiceGrpcClient;

    @InjectMocks
    private AdminConversionServiceImpl adminConversionService;

    @Test
    @DisplayName("Should get monthly conversion ledger successfully")
    void testGetMonthlyConversionLedger_Success_ReturnsLedger() {
        // Given
        int year = 2024;
        int month = 11;
        PageRequest pageable = PageRequest.of(0, 10);

        com.tuttime.common.proto.ConversionLedgerEntry entry1 = 
                com.tuttime.common.proto.ConversionLedgerEntry.newBuilder()
                .setUserId(100L)
                .setApAmount("1000.50")
                .setUserSharePercent("2.00")
                .setDistributedApartAmount("1000000")
                .build();

        com.tuttime.common.proto.ConversionLedgerEntry entry2 = 
                com.tuttime.common.proto.ConversionLedgerEntry.newBuilder()
                .setUserId(200L)
                .setApAmount("2000.75")
                .setUserSharePercent("4.00")
                .setDistributedApartAmount("2000000")
                .build();

        GetMonthlyConversionLedgerResponse grpcResponse = GetMonthlyConversionLedgerResponse.newBuilder()
                .addEntries(entry1)
                .addEntries(entry2)
                .setTotalPages(1)
                .setTotalElements(2)
                .setHasNext(false)
                .build();

        UserResponse user1 = UserResponse.newBuilder()
                .setUserId(100L)
                .setUsername("John Doe")
                .build();

        UserResponse user2 = UserResponse.newBuilder()
                .setUserId(200L)
                .setUsername("Jane Smith")
                .build();

        when(conversionServiceGrpcClient.getMonthlyConversionLedger(year, month, 0, 10))
                .thenReturn(grpcResponse);
        when(userServiceGrpcClient.getUserById(100L)).thenReturn(user1);
        when(userServiceGrpcClient.getUserById(200L)).thenReturn(user2);

        // When
        Page<com.tuttime.admin.dto.ConversionLedgerEntry> result = 
                adminConversionService.getMonthlyConversionLedger(year, month, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        
        com.tuttime.admin.dto.ConversionLedgerEntry dto1 = result.getContent().get(0);
        assertThat(dto1.getUserId()).isEqualTo(100L);
        assertThat(dto1.getUsername()).isEqualTo("John Doe");
        assertThat(dto1.getApAmount()).isEqualByComparingTo(new BigDecimal("1000.50"));
        assertThat(dto1.getUserSharePercent()).isEqualByComparingTo(new BigDecimal("2.00"));
        assertThat(dto1.getDistributedApartAmount()).isEqualByComparingTo(new BigDecimal("1000000"));

        com.tuttime.admin.dto.ConversionLedgerEntry dto2 = result.getContent().get(1);
        assertThat(dto2.getUserId()).isEqualTo(200L);
        assertThat(dto2.getUsername()).isEqualTo("Jane Smith");
    }

    @Test
    @DisplayName("Should handle missing username gracefully")
    void testGetMonthlyConversionLedger_MissingUsername_HandlesGracefully() {
        // Given
        int year = 2024;
        int month = 11;
        PageRequest pageable = PageRequest.of(0, 10);

        com.tuttime.common.proto.ConversionLedgerEntry entry = 
                com.tuttime.common.proto.ConversionLedgerEntry.newBuilder()
                .setUserId(100L)
                .setApAmount("1000.50")
                .setUserSharePercent("2.00")
                .setDistributedApartAmount("1000000")
                .build();

        GetMonthlyConversionLedgerResponse grpcResponse = GetMonthlyConversionLedgerResponse.newBuilder()
                .addEntries(entry)
                .setTotalPages(1)
                .setTotalElements(1)
                .setHasNext(false)
                .build();

        when(conversionServiceGrpcClient.getMonthlyConversionLedger(year, month, 0, 10))
                .thenReturn(grpcResponse);
        when(userServiceGrpcClient.getUserById(100L))
                .thenThrow(new RuntimeException("User not found"));

        // When
        Page<com.tuttime.admin.dto.ConversionLedgerEntry> result = 
                adminConversionService.getMonthlyConversionLedger(year, month, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getUsername()).isEmpty();
    }

    @Test
    @DisplayName("Should handle empty ledger")
    void testGetMonthlyConversionLedger_Empty_ReturnsEmpty() {
        // Given
        int year = 2024;
        int month = 11;
        PageRequest pageable = PageRequest.of(0, 10);

        GetMonthlyConversionLedgerResponse grpcResponse = GetMonthlyConversionLedgerResponse.newBuilder()
                .setTotalPages(0)
                .setTotalElements(0)
                .setHasNext(false)
                .build();

        when(conversionServiceGrpcClient.getMonthlyConversionLedger(year, month, 0, 10))
                .thenReturn(grpcResponse);

        // When
        Page<com.tuttime.admin.dto.ConversionLedgerEntry> result = 
                adminConversionService.getMonthlyConversionLedger(year, month, pageable);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(0);
        assertThat(result.getContent()).isEmpty();
    }
}

