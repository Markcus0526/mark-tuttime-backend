package com.tuttime.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security Configuration: minimal stateless configuration - gateway proxies requests, admin-service handles authentication.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:8080,https://tuttime-admin.frontare.ink}")
	private String allowedOrigins;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(csrf -> csrf.disable())
				// CORS handled manually via CorsHeaderFilter to avoid conflicts with reverse proxies
				.cors(cors -> cors.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.anyRequest().permitAll()
				);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		List<String> origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty() && !"*".equals(s))
				.distinct()
				.collect(Collectors.toList());

		if (origins.isEmpty()) {
			log.warn("No valid CORS origins configured, using defaults");
			origins = Arrays.asList("http://localhost:3000", "http://localhost:8080", "https://tuttime-admin.frontare.ink");
		}

		log.info("CORS allowed origins (deduplicated): {}", origins);

		if (origins.size() != origins.stream().distinct().count()) {
			log.error("CORS configuration error: Duplicate origins detected after processing! Original: {}, Processed: {}",
					allowedOrigins, origins);
			origins = origins.stream().distinct().collect(Collectors.toList());
		}

		configuration.setAllowedOrigins(origins);

		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);

		// Exposed headers for frontend
		configuration.setExposedHeaders(Arrays.asList(
				"Authorization",
				"Content-Type",
				"X-Total-Count",
				"X-Correlation-Id"
		));

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}

