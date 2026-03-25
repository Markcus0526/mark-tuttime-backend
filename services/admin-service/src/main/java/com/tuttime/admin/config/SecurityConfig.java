package com.tuttime.admin.config;

import com.tuttime.admin.filter.AdminIpWhitelistFilter;
import com.tuttime.admin.filter.AdminJwtAuthenticationFilter;
import com.tuttime.admin.filter.AdminRateLimitFilter;
import com.tuttime.admin.service.AdminAuthService;
import com.tuttime.admin.service.AdminTokenBlacklistService;
import com.tuttime.admin.service.AdminTokenWhitelistService;
import com.tuttime.admin.util.AdminJwtTokenUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Admin Security Configuration: configures security for admin service with JWT authentication, role-based access control, and comprehensive security filters.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

	private final AdminJwtTokenUtil jwtTokenUtil;
	private final AdminAuthService authService;
	private final AdminTokenWhitelistService tokenWhitelistService;
	private final AdminTokenBlacklistService tokenBlacklistService;
	private final AdminRateLimitFilter rateLimitFilter;
	private final CorsConfigurationSource corsConfigurationSource;
	private final IpWhitelistConfig ipWhitelistConfig;

	public SecurityConfig(
			AdminJwtTokenUtil jwtTokenUtil,
			@Lazy AdminAuthService authService,
			AdminTokenWhitelistService tokenWhitelistService,
			AdminTokenBlacklistService tokenBlacklistService,
			AdminRateLimitFilter rateLimitFilter,
			CorsConfigurationSource corsConfigurationSource,
			IpWhitelistConfig ipWhitelistConfig) {
		this.jwtTokenUtil = jwtTokenUtil;
		this.authService = authService;
		this.tokenWhitelistService = tokenWhitelistService;
		this.tokenBlacklistService = tokenBlacklistService;
		this.rateLimitFilter = rateLimitFilter;
		this.corsConfigurationSource = corsConfigurationSource;
		this.ipWhitelistConfig = ipWhitelistConfig;
	}

	@Bean
	public AdminJwtAuthenticationFilter adminJwtAuthenticationFilter() {
		return new AdminJwtAuthenticationFilter(
				jwtTokenUtil,
				authService,
				tokenWhitelistService,
				tokenBlacklistService
		);
	}

	@Bean
	public AdminIpWhitelistFilter adminIpWhitelistFilter() {
		return new AdminIpWhitelistFilter(ipWhitelistConfig);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.cors(cors -> cors.configurationSource(corsConfigurationSource))
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// Public endpoints
						.requestMatchers(
								"/api/admin/auth/login",
								"/api/admin/users/beta-tester/delete-by-phone",
								"/actuator/**",
								"/swagger-ui/**",
								"/v3/api-docs/**"
						).permitAll()
						// All other endpoints require authentication
						.requestMatchers("/api/admin/**").authenticated()
						.anyRequest().authenticated()
				)
				.addFilterBefore(adminIpWhitelistFilter(), UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
				.addFilterBefore(adminJwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}

