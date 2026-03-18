package com.tuttime.user.controller;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.RateLimitExceededException;
import com.tuttime.user.dto.UpdateLocationRequest;
import com.tuttime.user.service.RateLimitService;
import com.tuttime.user.service.UserLocationService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for UserLocationController error handling scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserLocationController Error Handling Tests")
class UserLocationControllerErrorTest {

    @Mock
    private UserLocationService userLocationService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private UserLocationController userLocationController;

    @BeforeEach
    void setUp() {
        lenient().when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        lenient().when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);
        lenient().when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
    }

    @Test
    @DisplayName("updateLocation should throw RateLimitExceededException when rate limit is exceeded")
    void testUpdateLocation_RateLimitExceeded_ThrowsRateLimitExceededException() {
        // Given
        Long userId = 1L;
        UpdateLocationRequest request = new UpdateLocationRequest();
        request.setLatitude(BigDecimal.valueOf(40.7128));
        request.setLongitude(BigDecimal.valueOf(-74.0060));

        when(rateLimitService.isLocationUpdateAllowed(userId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userLocationController.updateLocation(userId, request, httpRequest))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> {
                    RateLimitExceededException rateLimitEx = (RateLimitExceededException) ex;
                    assertThat(rateLimitEx.getMessage()).contains("Rate limit exceeded");
                    assertThat(rateLimitEx.getRetryAfterSeconds()).isEqualTo(3600);
                });
    }

    @Test
    @DisplayName("getUsersOnMap should throw RateLimitExceededException when map query rate limit is exceeded")
    void testGetUsersOnMap_RateLimitExceeded_ThrowsRateLimitExceededException() {
        // Given
        Long userId = 1L;
        when(rateLimitService.isMapQueryAllowed(userId, "192.168.1.1")).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> userLocationController.getUsersOnMap(
                userId, null, null, null, null, null, 100, 0, httpRequest))
                .isInstanceOf(RateLimitExceededException.class)
                .satisfies(ex -> {
                    RateLimitExceededException rateLimitEx = (RateLimitExceededException) ex;
                    assertThat(rateLimitEx.getMessage()).contains("Rate limit exceeded");
                    assertThat(rateLimitEx.getRetryAfterSeconds()).isEqualTo(60);
                });
    }

    @Test
    @DisplayName("getUsersOnMap should throw BusinessException when bounds format is invalid")
    void testGetUsersOnMap_InvalidBoundsFormat_ThrowsBusinessException() {
        // Given
        Long userId = 1L;
        String invalidBounds = "invalid-format";

        when(rateLimitService.isMapQueryAllowed(userId, "192.168.1.1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userLocationController.getUsersOnMap(
                userId, invalidBounds, null, null, null, null, 100, 0, httpRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid bounds format");
    }

    @Test
    @DisplayName("getUsersOnMap should throw BusinessException when center format is invalid")
    void testGetUsersOnMap_InvalidCenterFormat_ThrowsBusinessException() {
        // Given
        Long userId = 1L;
        String invalidCenter = "invalid-format";

        when(rateLimitService.isMapQueryAllowed(userId, "192.168.1.1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userLocationController.getUsersOnMap(
                userId, null, invalidCenter, null, null, null, 100, 0, httpRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid center format");
    }

    @Test
    @DisplayName("getUsersOnMap should throw BusinessException when nearby format is invalid")
    void testGetUsersOnMap_InvalidNearbyFormat_ThrowsBusinessException() {
        // Given
        Long userId = 1L;
        String invalidNearby = "invalid-format";

        when(rateLimitService.isMapQueryAllowed(userId, "192.168.1.1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userLocationController.getUsersOnMap(
                userId, null, null, invalidNearby, null, null, 100, 0, httpRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid nearby format");
    }

    @Test
    @DisplayName("getUsersOnMap should throw BusinessException when no query parameters provided")
    void testGetUsersOnMap_NoQueryParameters_ThrowsBusinessException() {
        // Given
        Long userId = 1L;

        when(rateLimitService.isMapQueryAllowed(userId, "192.168.1.1")).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> userLocationController.getUsersOnMap(
                userId, null, null, null, null, null, 100, 0, httpRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("One of bounds, center, nearby, or country parameter is required");
    }
}

