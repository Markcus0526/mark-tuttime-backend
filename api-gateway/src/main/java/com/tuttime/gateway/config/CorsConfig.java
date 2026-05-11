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

	@Value("${app.cors.allowed-origins}")
	private String allowedOrigins;

	@Bean
	public CorsWebFilter corsWebFilter() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		// ---------- WebSocket CORS (allows all origins, no credentials) ----------
		CorsConfiguration websocketConfig = new CorsConfiguration();
		websocketConfig.setAllowedOriginPatterns(List.of("*"));  // allow all origins
		websocketConfig.setAllowedMethods(Arrays.asList("GET", "OPTIONS"));
		websocketConfig.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "*"));
		websocketConfig.setAllowCredentials(false); // must be false with wildcard origin
		websocketConfig.setMaxAge(3600L);
		source.registerCorsConfiguration("/ws/**", websocketConfig);
		log.info("CORS configured for WebSocket endpoints (/ws/**) - allowing all origins, credentials disabled");

		// ---------- REST API CORS (explicit origins, credentials allowed) ----------
		CorsConfiguration apiConfig = new CorsConfiguration();

		// Parse allowed origins from comma-separated string
		List<String> origins = Arrays.asList(allowedOrigins.split(","));
		boolean allowAllOrigins = origins.stream().map(String::trim).anyMatch("*"::equals);
		List<String> specificOrigins = origins.stream()
				.map(String::trim)
				.filter(s -> !s.isEmpty() && !"*".equals(s))
				.toList();

		if (allowAllOrigins || specificOrigins.isEmpty()) {
			// Wildcard origin: cannot send credentials (CORS spec violation otherwise)
			apiConfig.setAllowedOriginPatterns(List.of("*"));
			apiConfig.setAllowCredentials(false);
			log.info("CORS: Wildcard origin allowed for REST APIs – credentials disabled (browsers will not send cookies/auth)");
		} else {
			// Specific origins: credentials allowed
			apiConfig.setAllowedOriginPatterns(specificOrigins);
			apiConfig.setAllowCredentials(true);
			log.info("CORS: Specific origins allowed for REST APIs: {} – credentials enabled", specificOrigins);
		}

		// Allowed HTTP methods
		apiConfig.setAllowedMethods(Arrays.asList(
				"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"
		));

		// Allowed headers (including device tracking for anti-fraud)
		apiConfig.setAllowedHeaders(Arrays.asList(
				"Authorization",
				"Content-Type",
				"X-Requested-With",
				"X-User-Id",
				"X-User-Roles",
				"X-User-Email",
				"X-Correlation-Id",
				"X-Request-ID",
				// Device tracking headers for anti-fraud
				"X-Device-Id",
				"X-Device-Type",
				"X-Device-Model",
				"X-OS-Version",
				"X-App-Version",
				"User-Agent",
				"Accept-Language",
				"Accept-Encoding",
				"X-Forwarded-For",
				"X-Real-IP",
				"X-Telegram-Account"
		));

		// Exposed headers (client can read)
		apiConfig.setExposedHeaders(Arrays.asList(
				"X-Correlation-Id",
				"X-Request-ID",
				"X-Rate-Limit-Remaining",
				"X-Rate-Limit-Reset"
		));

		apiConfig.setMaxAge(3600L); // cache preflight for 1 hour

		source.registerCorsConfiguration("/**", apiConfig);

		return new CorsWebFilter(source);
	}
}