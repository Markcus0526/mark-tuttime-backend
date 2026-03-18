package com.tuttime.admin.gateway.controller;

import com.tuttime.admin.gateway.service.AdminProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProxyController Tests")
class AdminProxyControllerTest {

    @Mock
    private AdminProxyService adminProxyService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AdminProxyController adminProxyController;

    @BeforeEach
    void setUp() {
        when(httpRequest.getRequestURI()).thenReturn("/api/admin/users");
    }

    @Test
    @DisplayName("Should proxy GET request to admin-service")
    void testProxyGet_Success_ProxiesRequest() {
        // Given
        String authorization = "Bearer token";
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok().build();
        when(adminProxyService.proxyRequest(any(HttpServletRequest.class), eq(authorization)))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<Object> response = adminProxyController.proxyGet(authorization, httpRequest);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(adminProxyService).proxyRequest(eq(httpRequest), eq(authorization));
    }

    @Test
    @DisplayName("Should proxy POST request to admin-service")
    void testProxyPost_Success_ProxiesRequest() {
        // Given
        String authorization = "Bearer token";
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok().build();
        when(adminProxyService.proxyRequest(any(HttpServletRequest.class), eq(authorization)))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<Object> response = adminProxyController.proxyPost(authorization, httpRequest);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(adminProxyService).proxyRequest(eq(httpRequest), eq(authorization));
    }

    @Test
    @DisplayName("Should proxy PUT request to admin-service")
    void testProxyPut_Success_ProxiesRequest() {
        // Given
        String authorization = "Bearer token";
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok().build();
        when(adminProxyService.proxyRequest(any(HttpServletRequest.class), eq(authorization)))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<Object> response = adminProxyController.proxyPut(authorization, httpRequest);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(adminProxyService).proxyRequest(eq(httpRequest), eq(authorization));
    }

    @Test
    @DisplayName("Should proxy DELETE request to admin-service")
    void testProxyDelete_Success_ProxiesRequest() {
        // Given
        String authorization = "Bearer token";
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok().build();
        when(adminProxyService.proxyRequest(any(HttpServletRequest.class), eq(authorization)))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<Object> response = adminProxyController.proxyDelete(authorization, httpRequest);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(adminProxyService).proxyRequest(eq(httpRequest), eq(authorization));
    }

    @Test
    @DisplayName("Should handle requests without authorization header")
    void testProxyGet_NoAuthorization_ProxiesRequest() {
        // Given
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok().build();
        when(adminProxyService.proxyRequest(any(HttpServletRequest.class), isNull()))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<Object> response = adminProxyController.proxyGet(null, httpRequest);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(adminProxyService).proxyRequest(eq(httpRequest), isNull());
    }
}

