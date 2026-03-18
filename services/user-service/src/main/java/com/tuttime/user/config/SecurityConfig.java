package com.tuttime.user.config;

import com.tuttime.user.filter.JwtAuthenticationFilter;
import com.tuttime.user.service.AuthService;
import com.tuttime.user.service.TokenWhitelistService;
import com.tuttime.user.util.JwtTokenUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenUtil jwtTokenUtil, 
            AuthService authService,
            TokenWhitelistService tokenWhitelistService) {
        return new JwtAuthenticationFilter(jwtTokenUtil, authService, tokenWhitelistService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                // Auth endpoints (public, except email verification)
                                "/api/v1/auth/signup/**",
                                "/api/v1/auth/signin",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/signout",
                                "/api/v1/auth/check-username", // Public: Check username availability (for signup)
                                "/api/v1/auth/check-email", // Public: Check email availability (for signup)
                                // Password management endpoints (public)
                                "/api/v1/password/**",
                                // Legacy user endpoints (for backward compatibility)
                                "/api/v1/users/register",
                                "/api/v1/users/login",
                                "/api/v1/users/refresh", // Allow refresh token endpoint (public)
                                "/api/v1/users/logout", // Allow logout without auth (handles expired tokens)
                                // Public user endpoints
                                "/api/v1/users/*/image", // Public user image access
                                // Public language endpoints (exclude /apple/* which requires auth)
                                "/api/v1/users/languages", // Public: Get available languages
                                "/api/v1/users/languages/all", // Public: Get all language translations
                                "/api/v1/users/languages/*", // Public: Get language translations by code (e.g., /en, /ru)
                                // Public phone mask endpoints
                                "/api/v1/users/phone-masks", // Public: Get all phone masks
                                "/api/v1/users/phone-masks/iso/*", // Public: Get phone mask by ISO code
                                "/api/v1/users/phone-masks/code/*", // Public: Get phone mask by phone code
                                // Public policy endpoints
                                "/api/v1/users/policies", // Public: Get available policy languages
                                "/api/v1/users/policies/all", // Public: Get all policy translations
                                "/api/v1/users/policies/*", // Public: Get policy by language code (e.g., /en, /ru)
                                "/api/v1/users/s3/**", // Public S3 file access endpoints
                                // WebSocket endpoint (authentication handled by interceptor)
                                "/ws/presence",
                                // Admin endpoints (public for internal service-to-service communication)
                                // Only allow the specific admin endpoints that user-service actually handles
                                "/api/v1/admin/users/**", // Admin user endpoints called by admin-service
                                "/api/v1/admin/devices/**", // Admin device endpoints called by admin-service
                                // Actuator and API docs
                                "/actuator/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Protected endpoints (authentication required)
                        .requestMatchers(
                                "/api/v1/auth/email/**", // Email verification requires authentication
                                "/api/v1/users/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Add JWT filter before UsernamePasswordAuthenticationFilter
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

