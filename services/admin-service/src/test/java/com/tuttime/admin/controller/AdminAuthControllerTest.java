package com.tuttime.admin.controller;

import com.tuttime.admin.dto.AdminLoginRequest;
import com.tuttime.admin.dto.AdminLoginResponse;
import com.tuttime.admin.service.AdminAuthService;
import com.tuttime.common.dto.ApiResponse;
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
@DisplayName("AdminAuthController Tests")
class AdminAuthControllerTest {

    @Mock
    private AdminAuthService adminAuthService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AdminAuthController adminAuthController;

    private AdminLoginRequest loginRequest;
    private AdminLoginResponse loginResponse;

    @BeforeEach
    void setUp() {
        loginRequest = new AdminLoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("password123");

        loginResponse = AdminLoginResponse.builder()
                .token("jwt-token")
                .adminId(1L)
                .username("admin")
                .role("ADMIN")
                .build();
    }

    @Test
    @DisplayName("Should login successfully")
    void testLogin_Success_ReturnsOk() {
        // Given
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn(null);
        when(httpRequest.getHeader("X-Real-IP")).thenReturn(null);
        when(httpRequest.getRemoteAddr()).thenReturn("192.168.1.1");
        when(adminAuthService.login(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(loginResponse);

        // When
        ResponseEntity<ApiResponse<AdminLoginResponse>> response = 
                adminAuthController.login(loginRequest, httpRequest);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isTrue();
        assertThat(response.getBody().getData()).isEqualTo(loginResponse);
        verify(adminAuthService).login(eq("admin"), eq("password123"), anyString(), eq("Mozilla/5.0"));
    }

    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void testLogin_ExtractsIpFromXForwardedFor() {
        // Given
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(adminAuthService.login(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(loginResponse);

        // When
        adminAuthController.login(loginRequest, httpRequest);

        // Then
        verify(adminAuthService).login(anyString(), anyString(), eq("10.0.0.1"), anyString());
    }

    @Test
    @DisplayName("Should extract IP from X-Real-IP header")
    void testLogin_ExtractsIpFromXRealIp() {
        // Given
        when(httpRequest.getHeader("User-Agent")).thenReturn("Mozilla/5.0");
        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("unknown");
        when(httpRequest.getHeader("X-Real-IP")).thenReturn("10.0.0.2");
        when(adminAuthService.login(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(loginResponse);

        // When
        adminAuthController.login(loginRequest, httpRequest);

        // Then
        verify(adminAuthService).login(anyString(), anyString(), eq("10.0.0.2"), anyString());
    }

    @Test
    @DisplayName("Should logout successfully")
    void testLogout_Success_ReturnsOk() {
        // Given
        Long adminId = 1L;
        String authorizationHeader = "Bearer jwt-token";

        // When
        ResponseEntity<ApiResponse<Void>> response = 
                adminAuthController.logout(adminId, authorizationHeader);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isTrue();
        verify(adminAuthService).logout(eq("jwt-token"), eq(adminId));
    }

    @Test
    @DisplayName("Should handle logout with Bearer prefix")
    void testLogout_WithBearerPrefix_ExtractsToken() {
        // Given
        Long adminId = 1L;
        String authorizationHeader = "Bearer jwt-token-123";

        // When
        adminAuthController.logout(adminId, authorizationHeader);

        // Then
        verify(adminAuthService).logout(eq("jwt-token-123"), eq(adminId));
    }
}

