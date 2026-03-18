package com.tuttime.admin.gateway.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("Should create CORS configuration source")
    void testCorsConfigurationSource_ShouldCreateBean() {
        // When
        CorsConfigurationSource corsConfigurationSource = securityConfig.corsConfigurationSource();

        // Then
        assertThat(corsConfigurationSource).isNotNull();
    }

    @Test
    @DisplayName("SecurityConfig should be annotated with EnableWebSecurity")
    void testSecurityConfig_ShouldBeAnnotated() {
        // Then
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableWebSecurity.class))
                .as("SecurityConfig should be annotated with @EnableWebSecurity")
                .isTrue();
    }
}

