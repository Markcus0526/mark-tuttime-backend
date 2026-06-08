package com.tuttime.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http,
                                                         CorsConfigurationSource corsConfigurationSource) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeExchange(exchanges -> exchanges
                        // 🛑 DECLARATIVE PERMIT-ALL RULES: Open the front gates ONLY for whitelisted public traffic
                        .pathMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/*/v3/api-docs").permitAll()
                        .pathMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/register",
                                "/api/v1/auth/oauth-login",
                                "/api/v1/auth/password-reset/request",
                                "/api/v1/auth/password-reset/confirm",
                                "/api/v1/auth/otp/request",
                                "/api/v1/auth/otp/verify",
                                "/api/v1/auth/device-token").permitAll()
                        .pathMatchers("/ws/notifications/**").permitAll()
                        .pathMatchers(
                                "/user-medias/**",
                                "/restaurant-medias/**",
                                "/menu-medias/**").permitAll()

                        // 🛑 ZERO-TRUST ENFORCEMENT: All other endpoints require authentication
                        .anyExchange().permitAll() // Spring Security passes through, allowing your Gateway Filter below to evaluate tokens
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "X-User-Id", "X-User-Roles"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
