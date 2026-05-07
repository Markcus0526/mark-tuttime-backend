package com.tuttime.gateway.filter;

import com.tuttime.gateway.service.TokenValidationService;
import com.tuttime.gateway.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * JWT Authentication Filter: validates JWT tokens, checks whitelist/blacklist,
 * validates ISS/AUD, adds user info to headers.
 * <p>
 * Supports method‑aware public endpoint rules (e.g., GET public, POST protected).
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

	private final JwtTokenUtil jwtTokenUtil;
	private final TokenValidationService tokenValidationService;

	public JwtAuthenticationFilter(JwtTokenUtil jwtTokenUtil, TokenValidationService tokenValidationService) {
		super(Config.class);
		this.jwtTokenUtil = jwtTokenUtil;
		this.tokenValidationService = tokenValidationService;
	}

	@Override
	public String name() {
		return "JwtAuthentication";
	}

	/**
	 * Rule for public endpoints: path prefix + allowed HTTP methods.
	 */
	private record PublicEndpointRule(String pathPrefix, List<String> allowedMethods) {
		boolean matches(String path, String method) {
			return path.startsWith(pathPrefix) && allowedMethods.contains(method);
		}
	}

	/**
	 * Public endpoint rules: path prefix and which HTTP methods are allowed without JWT.
	 */
	private static final List<PublicEndpointRule> PUBLIC_ENDPOINT_RULES = Arrays.asList(
			// Health & monitoring (all methods)
			new PublicEndpointRule("/health", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/api/v1/health", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/actuator", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),

			// medias(user, restaurant, menu)
			new PublicEndpointRule("/user-medias", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/restaurant-medias", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/menu-medias", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),

			// user-service
			new PublicEndpointRule("/api/v1/auth/device-token", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/login", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/logout", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/login", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/oauth-login", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/otp/request", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/otp/verify", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/password-reset/confirm", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/password-reset/request", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/refresh-token", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/register", List.of("POST")),

			// restaurant-service
			new PublicEndpointRule("/api/v1/restaurants", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurants/", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-branches", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-branches/", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-branches/near", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-branches/restaurants/", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-branches/tags", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-zones/", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-zones/branches/", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-tables/", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-tables/zones/", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-schedules/", List.of("GET")),
			new PublicEndpointRule("/api/v1/restaurant-medias/", List.of("GET")),

			// menu-service
			new PublicEndpointRule("/api/v1/menus/", List.of("GET")),
			new PublicEndpointRule("/api/v1/menus/branches/", List.of("GET")),
			new PublicEndpointRule("/api/v1/menu-items/", List.of("GET")),
			new PublicEndpointRule("/api/v1/menu-items/categories/", List.of("GET")),
			new PublicEndpointRule("/api/v1/menu-items/tags", List.of("GET"))
	);

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			String path = request.getURI().getPath();
			String method = request.getMethod().name();

			log.debug("JWT Filter: Checking path: {}, method: {}", path, method);

			// Skip authentication for OPTIONS requests (CORS preflight)
			if ("OPTIONS".equalsIgnoreCase(method)) {
				log.debug("JWT Filter: OPTIONS request detected (CORS preflight), skipping authentication");
				return chain.filter(exchange);
			}

			List<String> skipPaths = config.getSkipPathsList();
			if (isPublicEndpoint(path, method, skipPaths)) {
				log.debug("JWT Filter: Public endpoint (method {}) detected, skipping authentication for: {}", method, path);
				return chain.filter(exchange);
			}

			log.debug("JWT Filter: Protected endpoint, requiring authentication for: {}", path);

			String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
			}

			String token = authHeader.substring(7);

			return validateTokenReactive(token)
					.flatMap(isValid -> {
						if (!isValid) {
							log.debug("JWT Filter: Token validation failed");
							return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
						}

						try {
							UUID userId = jwtTokenUtil.getUserIdFromToken(token);
							String email = jwtTokenUtil.getEmailFromToken(token);
							String role = jwtTokenUtil.getRoleFromToken(token);

							if (userId == null || email == null) {
								log.warn("JWT Filter: Failed to extract user information from token");
								return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
							}

							ServerHttpRequest modifiedRequest = request.mutate()
									.header("X-User-Id", userId.toString())
									.header("X-User-Email", email)
									.header("X-User-Role", role != null ? role : "")
									.build();

							log.debug("JWT Filter: Token validated successfully for user: {} ({})", userId, email);
							return chain.filter(exchange.mutate().request(modifiedRequest).build());
						} catch (Exception e) {
							log.error("JWT Filter: Error processing validated token: {}", e.getMessage(), e);
							return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
						}
					})
					.onErrorResume(e -> {
						log.error("JWT Filter: Unexpected error during token validation: {}", e.getMessage(), e);
						return onError(exchange, "Authentication failed", HttpStatus.INTERNAL_SERVER_ERROR);
					});
		};
	}

	/**
	 * Checks if the given path and method should skip JWT authentication.
	 */
	private boolean isPublicEndpoint(String path, String method, List<String> skipPaths) {
		// Check route-specific skip paths (from filter config)
		if (skipPaths != null && !skipPaths.isEmpty()) {
			for (String skipPath : skipPaths) {
				// Support wildcard patterns (e.g., /media/**)
				if (path.startsWith(skipPath.replace("*", ""))) {
					return true;
				}
			}
		}

		// Check default public endpoint rules (path prefix + allowed methods)
		for (PublicEndpointRule rule : PUBLIC_ENDPOINT_RULES) {
			if (rule.matches(path, method)) {
				return true;
			}
		}

		// Additional dynamic rules
		if (method.equals("GET") && path.matches("^/api/v1/users/[^/]+/image.*")) {
			return true;
		}

		return false;
	}

	/**
	 * Validate token: extract JTI, check whitelist/blacklist, validate ISS/AUD, signature, expiration.
	 * Whitelist and blacklist both fail‑closed for maximum security.
	 */
	private Mono<Boolean> validateTokenReactive(String token) {
		try {
			log.debug("JWT Filter: Starting token validation");

			String jti = jwtTokenUtil.getJtiFromToken(token);
			if (jti == null) {
				log.warn("JWT Filter: Token missing JTI claim - token may be invalid");
				return Mono.just(false);
			}
			log.debug("JWT Filter: Extracted JTI: {}", jti);

			// Local validation (fast, no Redis)
			boolean issuerValid = jwtTokenUtil.validateIssuer(token);
			if (!issuerValid) {
				log.warn("JWT Filter: Token issuer validation failed");
				return Mono.just(false);
			}

			boolean audienceValid = jwtTokenUtil.validateAudience(token);
			if (!audienceValid) {
				log.warn("JWT Filter: Token audience validation failed");
				return Mono.just(false);
			}

			boolean tokenValid = jwtTokenUtil.validateToken(token);
			if (!tokenValid) {
				log.warn("JWT Filter: Token signature or expiration validation failed");
				return Mono.just(false);
			}
			log.debug("JWT Filter: Local validation passed");

			// Parallel Redis checks: both fail‑closed (return false / blacklisted on error)
			Mono<Boolean> whitelistCheck = tokenValidationService.isTokenWhitelisted(jti)
					.timeout(Duration.ofMillis(2000))
					.doOnError(e -> log.warn("Whitelist check error (fail‑closed): jti={}, error={}", jti, e.getMessage()))
					.onErrorReturn(false);   // Fail‑closed: if error, token considered NOT whitelisted

			Mono<Boolean> blacklistCheck = tokenValidationService.isTokenBlacklisted(token)
					.timeout(Duration.ofMillis(2000))
					.doOnError(e -> log.warn("Blacklist check error (fail‑closed): jti={}, assuming blacklisted", jti))
					.onErrorReturn(true);    // Fail‑closed: if error, token considered blacklisted

			return Mono.zip(whitelistCheck, blacklistCheck)
					.flatMap(tuple -> {
						Boolean isWhitelisted = tuple.getT1();
						Boolean isBlacklisted = tuple.getT2();

						if (!isWhitelisted) {
							log.warn("JWT Filter: Token not whitelisted: jti={}", jti);
							return Mono.just(false);
						}
						log.debug("JWT Filter: Token is whitelisted: jti={}", jti);

						if (isBlacklisted) {
							log.warn("JWT Filter: Token is blacklisted: jti={}", jti);
							return Mono.just(false);
						}
						log.debug("JWT Filter: Token not blacklisted: jti={}", jti);

						log.debug("JWT Filter: Token validation successful for jti={}", jti);
						return Mono.just(true);
					})
					.onErrorResume(e -> {
						log.error("JWT Filter: Error combining whitelist/blacklist: {}", e.getMessage(), e);
						return Mono.just(false);
					});
		} catch (Exception e) {
			log.error("JWT Filter: Token validation exception: {}", e.getMessage(), e);
			return Mono.just(false);
		}
	}

	/**
	 * Creates an error response when authentication fails.
	 */
	private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(status);
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

		String body = String.format("{\"error\":\"%s\",\"status\":%d}", message, status.value());
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
	}

	/**
	 * Configuration class for JWT Authentication Filter.
	 * Supports comma‑separated skipPaths (e.g., "/media/**,/public/**").
	 */
	public static class Config {
		private String skipPaths;

		public String getSkipPaths() {
			return skipPaths;
		}

		public void setSkipPaths(String skipPaths) {
			this.skipPaths = skipPaths;
		}

		public List<String> getSkipPathsList() {
			if (skipPaths == null || skipPaths.trim().isEmpty()) {
				return List.of();
			}
			return Arrays.stream(skipPaths.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.toList();
		}
	}
}