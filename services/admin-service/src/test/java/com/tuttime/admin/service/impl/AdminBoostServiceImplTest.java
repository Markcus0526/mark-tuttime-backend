package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.BookingBoostResponse;
import com.tuttime.admin.rest.client.MiningServiceRestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminBoostServiceImpl Tests")
class AdminBoostServiceImplTest {

    @Mock
    private MiningServiceRestClient miningServiceRestClient;

    @InjectMocks
    private AdminBoostServiceImpl adminBoostService;

    @Test
    @DisplayName("Should list booking boosts successfully")
    void testListBookingBoosts_Success_ReturnsPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        BookingBoostResponse response = BookingBoostResponse.builder()
                .boostId(1L)
                .userId(100L)
                .bookingId(200L)
                .boostAmount(new BigDecimal("2.0"))
                .isActive(true)
                .build();

        Page<BookingBoostResponse> page = new PageImpl<>(Arrays.asList(response), pageable, 1);
        when(miningServiceRestClient.listBookingBoosts(pageable, null)).thenReturn(page);

        // When
        Page<BookingBoostResponse> result = adminBoostService.listBookingBoosts(pageable, null);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("Should get booking boost by ID successfully")
    void testGetBookingBoost_Success_ReturnsBoost() {
        // Given
        Long id = 1L;
        BookingBoostResponse response = BookingBoostResponse.builder()
                .boostId(id)
                .userId(100L)
                .bookingId(200L)
                .boostAmount(new BigDecimal("2.0"))
                .build();

        when(miningServiceRestClient.getBookingBoost(id)).thenReturn(response);

        // When
        BookingBoostResponse result = adminBoostService.getBookingBoost(id);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getBoostId()).isEqualTo(id);
    }

    @Test
    @DisplayName("Should list rental boosts successfully")
    void testListRentalBoosts_Success_ReturnsPage() {
        // Given
        PageRequest pageable = PageRequest.of(0, 10);
        BookingBoostResponse response = BookingBoostResponse.builder()
                .boostId(1L)
                .userId(100L)
                .bookingId(200L)
                .boostAmount(new BigDecimal("2.0"))
                .build();

        com.tuttime.admin.dto.RentalBoostResponse rentalResponse = com.tuttime.admin.dto.RentalBoostResponse.builder()
                .boostId(1L)
                .userId(100L)
                .rentalId(200L)
                .boostAmount(new BigDecimal("2.0"))
                .isActive(true)
                .build();

        Page<com.tuttime.admin.dto.RentalBoostResponse> page = new PageImpl<>(Arrays.asList(rentalResponse), pageable, 1);
        when(miningServiceRestClient.listRentalBoosts(pageable, null)).thenReturn(page);

        // When
        Page<com.tuttime.admin.dto.RentalBoostResponse> result = adminBoostService.listRentalBoosts(pageable, null);

        // Then
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should get boosts by user ID successfully")
    void testGetBoostsByUserId_Success_ReturnsList() {
        // Given
        Long userId = 100L;
        BookingBoostResponse response = BookingBoostResponse.builder()
                .boostId(1L)
                .userId(userId)
                .bookingId(200L)
                .boostAmount(new BigDecimal("2.0"))
                .build();

        when(miningServiceRestClient.getBoostsByUserId(userId))
                .thenReturn(Arrays.asList(response));

        // When
        List<Object> result = adminBoostService.getBoostsByUserId(userId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0)).isInstanceOf(BookingBoostResponse.class);
        BookingBoostResponse boostResponse = (BookingBoostResponse) result.get(0);
        assertThat(boostResponse.getUserId()).isEqualTo(userId);
    }
}

