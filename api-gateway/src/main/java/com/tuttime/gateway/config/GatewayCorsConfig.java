package com.tuttime.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
// 1. Notice the REACTIVE imports here:
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class GatewayCorsConfig {

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();

		// ... Your existing list processing logic remains perfectly fine ...
		List<String> origins = Arrays.asList("http://localhost:3000", "https://tuttime-admin.frontare.ink");
		configuration.setAllowedOrigins(origins);

		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
		configuration.setAllowedHeaders(Arrays.asList("*"));
		configuration.setAllowCredentials(true);
		configuration.setMaxAge(3600L);
		configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Total-Count", "X-Correlation-Id"));

		// 2. Use the Reactive Source
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}