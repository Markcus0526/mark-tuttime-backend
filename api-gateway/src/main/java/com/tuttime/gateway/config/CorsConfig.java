package com.tuttime.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS Configuration for API Gateway
 *
 * <p>Configures Cross-Origin Resource Sharing (CORS) to allow
 * mobile app and web clients to access the API.
 */
@Slf4j
@Configuration
public class CorsConfig {

	@Value("${app.cors.allowed-origins:https://app.tuttime.com,http://localhost:3000,http://localhost:8080}")
	private String allowedOrigins;

	@Bean
	public CorsWebFilter corsWebFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		// CORS configuration for WebSocket endpoints - allow all origins
		// WebSocket connections don't use CORS preflight, but Spring Gateway checks Origin header
		CorsConfiguration websocketConfig = new CorsConfiguration();
		websocketConfig.setAllowedOrigins(Arrays.asList("*")); // Allow all origins for WebSocket
		websocketConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
		// Explicitly allow Authorization header for WebSocket connections
		websocketConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "*"));
		websocketConfig.setAllowCredentials(false); // Must be false when using "*" origin
		websocketConfig.setMaxAge(3600L);
		source.registerCorsConfiguration("/ws/**", websocketConfig);
		log.info("CORS configured for WebSocket endpoints (/ws/**) - allowing all origins and Authorization header");

		// CORS configuration for REST API endpoints
		CorsConfiguration apiConfig = new CorsConfiguration();

		// Parse allowed origins from comma-separated string
		List<String> origins = Arrays.asList(allowedOrigins.split(","));

		// Check if "*" is explicitly configured (allow all origins)
		boolean allowAllOrigins = origins.stream()
				.map(String::trim)
				.anyMatch(s -> "*".equals(s));

		List<String> processedOrigins = origins.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty() && !"*".equals(s))
				.toList();

		// Configure CORS based on whether "*" is set
		if (allowAllOrigins || processedOrigins.isEmpty()) {
			// If "*" is configured or no origins specified, allow ALL origins
			// This includes mobile apps that may not send Origin headers
			apiConfig.setAllowedOrigins(Arrays.asList("*"));
			apiConfig.setAllowCredentials(false); // Must be false when using "*" (CORS spec)
			log.info("CORS configured to allow ALL origins (*) - mobile apps supported, credentials disabled");
		} else {
			// Specific origins configured - use origin patterns for flexibility
			// setAllowedOriginPatterns() is more flexible and handles edge cases better
			apiConfig.setAllowedOriginPatterns(processedOrigins);
			apiConfig.setAllowCredentials(true);
			log.info("CORS configured with specific origins: {} - credentials enabled", processedOrigins);
		}

		// Allowed HTTP methods
		apiConfig.setAllowedMethods(Arrays.asList(
				"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
		));

		// Allowed headers - includes device tracking headers for anti-fraud
		apiConfig.setAllowedHeaders(Arrays.asList(
				"Authorization",
				"Content-Type",
				"X-Requested-With",
				"X-User-Id",
				"X-User-Email",
				"X-Correlation-Id",
				"X-Request-ID",
				// Device tracking headers for anti-fraud and device fingerprinting
				"X-Device-Id",
				"X-Device-Type",
				"X-Device-Model",
				"X-OS-Version",
				"X-App-Version",
				// Standard headers that may be used for device fingerprinting
				"User-Agent",
				"Accept-Language",
				"Accept-Encoding",
				// IP forwarding headers (may be added by proxies/load balancers)
				"X-Forwarded-For",
				"X-Real-IP",
				// Additional headers
				"X-Telegram-Account"
		));

		// Exposed headers
		apiConfig.setExposedHeaders(Arrays.asList(
				"X-Correlation-Id",
				"X-Request-ID",
				"X-Rate-Limit-Remaining",
				"X-Rate-Limit-Reset"
		));

		// Cache preflight requests for 1 hour
		apiConfig.setMaxAge(3600L);

		source.registerCorsConfiguration("/**", apiConfig);

		return new CorsWebFilter(source);
	}
}

