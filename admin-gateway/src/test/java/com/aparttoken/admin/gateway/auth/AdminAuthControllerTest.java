package com.tuttime.admin.gateway.auth;

import com.tuttime.admin.gateway.service.AdminProxyService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuthController Tests")
class AdminAuthControllerTest {

    @Mock
    private AdminProxyService adminProxyService;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AdminAuthController adminAuthController;

    @Test
    @DisplayName("Should proxy login request to admin-service")
    void testLogin_Success_ProxiesRequest() {
        // Given
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok()
                .body(Map.of("success", true, "data", Map.of("token", "jwt-token")));
        
        when(adminProxyService.proxyRequest(any(HttpServletRequest.class), isNull()))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<Object> response = adminAuthController.login(httpRequest);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(adminProxyService).proxyRequest(eq(httpRequest), isNull());
    }

    @Test
    @DisplayName("Should proxy logout request to admin-service")
    void testLogout_Success_ProxiesRequest() {
        // Given
        String authorization = "Bearer jwt-token";
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok()
                .body(Map.of("success", true));
        
        when(adminProxyService.proxyRequest(any(HttpServletRequest.class), eq(authorization)))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<Object> response = adminAuthController.logout(authorization, httpRequest);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(adminProxyService).proxyRequest(eq(httpRequest), eq(authorization));
    }

    @Test
    @DisplayName("Should handle login request")
    void testLogin_ProxiesRequest() {
        // Given
        ResponseEntity<Object> expectedResponse = ResponseEntity.ok().build();
        when(adminProxyService.proxyRequest(any(HttpServletRequest.class), isNull()))
                .thenReturn(expectedResponse);

        // When
        ResponseEntity<Object> response = adminAuthController.login(httpRequest);

        // Then
        assertThat(response).isEqualTo(expectedResponse);
        verify(adminProxyService).proxyRequest(eq(httpRequest), isNull());
    }
}

