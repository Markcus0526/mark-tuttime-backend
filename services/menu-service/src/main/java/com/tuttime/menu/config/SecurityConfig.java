package com.tuttime.menu.config;

import com.tuttime.common.auth.security.GatewayHeaderSecurityFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security Configuration: minimal stateless configuration - gateway proxies requests, admin-service handles authentication.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private final GatewayHeaderSecurityFilter gatewayHeaderSecurityFilter;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
				)
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/menu-medias/**").permitAll()
						.requestMatchers(
								"/api/v1/menus/**",
								"/api/v1/menu-items/**",
								"/api/v1/menu-modifiers/**",
								"/api/v1/menu-discounts/**",
								"/api/v1/menu-stoplists/**",
								"/api/v1/menu-medias/**").permitAll() // Add /v1
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**").permitAll()
				)
				.addFilterBefore(gatewayHeaderSecurityFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}
}

