package com.tuttime.admin.gateway.service;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminProxyServiceImpl Tests")
class AdminProxyServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private AdminProxyServiceImpl adminProxyService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminProxyService, "adminServiceUrl", "http://admin-service");
        ReflectionTestUtils.setField(adminProxyService, "restTemplate", restTemplate);
        
        when(httpRequest.getRequestURI()).thenReturn("/api/admin/users");
        when(httpRequest.getQueryString()).thenReturn(null);
        when(httpRequest.getMethod()).thenReturn("GET");
        when(httpRequest.getHeaderNames()).thenReturn(new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return false;
            }

            @Override
            public String nextElement() {
                return null;
            }
        });
    }

    @Test
    @DisplayName("Should proxy GET request successfully")
    void testProxyRequest_Get_Success() {
        // Given
        String authorization = "Bearer token";
        ResponseEntity<String> stringResponse = ResponseEntity.ok().body("{\"success\":true}");
        when(restTemplate.exchange(
                any(java.net.URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(stringResponse);

        // When
        ResponseEntity<Object> response = adminProxyService.proxyRequest(httpRequest, authorization);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"success\":true}");
        verify(restTemplate).exchange(
                any(java.net.URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    @DisplayName("Should proxy POST request with body successfully")
    void testProxyRequest_PostWithBody_Success() {
        // Given
        String authorization = "Bearer token";
        Map<String, String> body = new HashMap<>();
        body.put("key", "value");
        
        when(httpRequest.getMethod()).thenReturn("POST");
        ResponseEntity<String> stringResponse = ResponseEntity.ok().body("{\"success\":true}");
        when(restTemplate.exchange(
                any(java.net.URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(stringResponse);

        // When
        ResponseEntity<Object> response = adminProxyService.proxyRequest(httpRequest, authorization, body);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"success\":true}");
        verify(restTemplate).exchange(
                any(java.net.URI.class),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    @DisplayName("Should handle request with query parameters")
    void testProxyRequest_WithQueryParams_Success() {
        // Given
        String authorization = "Bearer token";
        when(httpRequest.getQueryString()).thenReturn("page=1&size=10");
        ResponseEntity<String> stringResponse = ResponseEntity.ok().body("{\"success\":true}");
        when(restTemplate.exchange(
                any(java.net.URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(stringResponse);

        // When
        ResponseEntity<Object> response = adminProxyService.proxyRequest(httpRequest, authorization);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("{\"success\":true}");
        verify(restTemplate).exchange(
                any(java.net.URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    @DisplayName("Should handle proxy request error gracefully")
    void testProxyRequest_Error_ReturnsErrorResponse() {
        // Given
        String authorization = "Bearer token";
        when(restTemplate.exchange(
                any(java.net.URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenThrow(new RuntimeException("Connection error"));

        // When
        ResponseEntity<Object> response = adminProxyService.proxyRequest(httpRequest, authorization);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        verify(restTemplate).exchange(
                any(java.net.URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        );
    }

    @Test
    @DisplayName("Should copy headers from request")
    void testProxyRequest_CopiesHeaders() {
        // Given
        String authorization = "Bearer token";
        Enumeration<String> headerNames = new Enumeration<String>() {
            private int index = 0;
            private final String[] headers = {"Content-Type", "Accept"};

            @Override
            public boolean hasMoreElements() {
                return index < headers.length;
            }

            @Override
            public String nextElement() {
                return headers[index++];
            }
        };
        
        when(httpRequest.getHeaderNames()).thenReturn(headerNames);
        when(httpRequest.getHeader("Content-Type")).thenReturn("application/json");
        when(httpRequest.getHeader("Accept")).thenReturn("application/json");
        
        ResponseEntity<String> stringResponse = ResponseEntity.ok().body("{\"success\":true}");
        when(restTemplate.exchange(
                any(java.net.URI.class),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(stringResponse);

        // When
        adminProxyService.proxyRequest(httpRequest, authorization);

        // Then
        verify(restTemplate).exchange(
                any(java.net.URI.class),
                eq(HttpMethod.GET),
                argThat(httpEntity -> {
                    HttpHeaders headers = ((HttpEntity<?>) httpEntity).getHeaders();
                    return headers.containsKey("Content-Type") && 
                           headers.containsKey("Accept") &&
                           headers.containsKey("Authorization");
                }),
                eq(String.class)
        );
    }
}

