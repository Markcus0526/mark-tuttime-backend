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
 * JWT Authentication Filter: validates JWT tokens, checks whitelist/blacklist, validates ISS/AUD, adds user info to headers.
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
	 * Default public endpoints that don't require JWT authentication
	 */
	private static final List<String> DEFAULT_PUBLIC_ENDPOINTS = Arrays.asList(
			"/health",
			"/api/v1/health",
			"/api/v1/auth/signup",
			"/api/v1/auth/signup/send-sms",
			"/api/v1/auth/signup/verify-sms",
			"/api/v1/auth/signin",
			"/api/v1/auth/refresh",
			"/api/v1/auth/check-username",
			"/api/v1/auth/check-email",
			"/api/v1/password/forgot",
			"/api/v1/password/verify-sms",
			"/api/v1/password/reset",
			"/api/v1/users/",
			"/api/v1/mining/status/public",
			"/actuator"
	);

	/**
	 * Applies JWT authentication: checks public endpoints, extracts token, validates, adds user info to headers.
	 */
	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			String path = request.getURI().getPath();
			String method = request.getMethod().name();

			log.debug("JWT Filter: Checking path: {}, method: {}", path, method);

			// Skip authentication for OPTIONS requests (CORS preflight)
			if ("OPTIONS".equals(method)) {
				log.debug("JWT Filter: OPTIONS request detected (CORS preflight), skipping authentication for: {}", path);
				return chain.filter(exchange);
			}

			List<String> skipPaths = config.getSkipPathsList();
			if (isPublicEndpoint(path, skipPaths)) {
				log.debug("JWT Filter: Public endpoint detected, skipping authentication for: {}", path);
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
							String roles = jwtTokenUtil.getRolesFromToken(token);

							if (userId == null || email == null) {
								log.warn("JWT Filter: Failed to extract user information from token");
								return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
							}

							ServerHttpRequest modifiedRequest = request.mutate()
									.header("X-User-Id", userId.toString())
									.header("X-User-Email", email)
									.build();

							log.debug("JWT Filter: Token validated successfully for user: {} ({})", userId, email);
							return chain.filter(exchange.mutate().request(modifiedRequest).build());
						} catch (Exception e) {
							log.error("JWT Filter: Error processing validated token: {}", e.getMessage());
							return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
						}
					})
					.onErrorResume(e -> {
						// Only catch token validation errors, not routing errors
						String errorMessage = e.getMessage();
						if (errorMessage != null && (
								errorMessage.contains("No servers available") ||
										errorMessage.contains("METHOD_NOT_ALLOWED") ||
										errorMessage.contains("Service") ||
										errorMessage.contains("routing")
						)) {
							log.warn("JWT Filter: Routing error detected (not a token validation error), propagating: {}", errorMessage);
							return Mono.error(e);
						}

						log.error("JWT Filter: Token validation error: {}", errorMessage);
						return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
					});
		};
	}

	/**
	 * Checks if the given path is a public endpoint that doesn't require authentication.
	 */
	private boolean isPublicEndpoint(String path, List<String> skipPaths) {
		// Check route-specific skip paths first
		if (skipPaths != null && !skipPaths.isEmpty()) {
			for (String skipPath : skipPaths) {
				// Support wildcard patterns like /api/v1/users/*/image
				String pattern = skipPath.replace("*", ".*");
				if (path.matches("^" + pattern + ".*") || path.startsWith(skipPath.replace("*", ""))) {
					return true;
				}
			}
		}

		// Check default public endpoints
		for (String publicPath : DEFAULT_PUBLIC_ENDPOINTS) {
			if (path.startsWith(publicPath)) {
				return true;
			}
		}

		// Check for user image endpoints (pattern: /api/v1/users/{userId}/image)
		if (path.matches("^/api/v1/users/\\d+/image.*")) {
			return true;
		}

		return false;
	}

	/**
	 * Validate token: extract JTI, check whitelist/blacklist, validate ISS/AUD, validate signature/expiration.
	 */
	private Mono<Boolean> validateTokenReactive(String token) {
		try {
			log.debug("JWT Filter: Starting token validation");

			String jti = jwtTokenUtil.getJtiFromToken(token);
			if (jti == null) {
				log.warn("JWT Filter: Token missing JTI claim - token may be invalid or malformed");
				return Mono.just(false);
			}
			log.debug("JWT Filter: Extracted JTI: {}", jti);

			// Validate JWT signature and expiration first (local, fast) before Redis checks
			// This fails fast for invalid/expired tokens without making Redis calls
			boolean issuerValid = jwtTokenUtil.validateIssuer(token);
			if (!issuerValid) {
				log.warn("JWT Filter: Token issuer validation failed - check issuer configuration");
				return Mono.just(false);
			}

			boolean audienceValid = jwtTokenUtil.validateAudience(token);
			if (!audienceValid) {
				log.warn("JWT Filter: Token audience validation failed - check audience configuration");
				return Mono.just(false);
			}

			boolean tokenValid = jwtTokenUtil.validateToken(token);
			if (!tokenValid) {
				log.warn("JWT Filter: Token signature or expiration validation failed - token may be expired or signature invalid");
				return Mono.just(false);
			}
			log.debug("JWT Filter: Token signature and expiration validation passed");

			// Run Redis checks in parallel for better performance
			// Fix 3: Increased whitelist timeout from 500ms to 2000ms and changed to fail-open.
			// The previous 500ms timeout with fail-closed caused false 401 rejections during Redis
			// latency spikes, triggering unnecessary token refresh attempts that compounded
			// the concurrent refresh race condition (Fix 1).
			// Whitelist check: fail-open (true) on timeout/error to prevent false rejections
			Mono<Boolean> whitelistCheck = tokenValidationService.isTokenWhitelisted(jti)
					.timeout(Duration.ofMillis(2000))
					.doOnError(e -> log.warn("JWT Filter: Whitelist check failed (fail-open, allowing request): jti={}, error={}", jti, e.getMessage()))
					.onErrorReturn(true);

			// Blacklist check: fail-closed (false) on timeout/error for security
			Mono<Boolean> blacklistCheck = tokenValidationService.isTokenBlacklisted(token)
					.timeout(Duration.ofMillis(2000))
					.onErrorReturn(false);

			// Combine both checks in parallel
			return Mono.zip(whitelistCheck, blacklistCheck)
					.flatMap(tuple -> {
						Boolean isWhitelisted = tuple.getT1();
						Boolean isBlacklisted = tuple.getT2();

						if (!isWhitelisted) {
							log.warn("JWT Filter: Token is not whitelisted: jti={}. Token may not have been properly stored in Redis or Redis may be unavailable.", jti);
							return Mono.just(false);
						}
						log.debug("JWT Filter: Token is whitelisted: jti={}", jti);

						if (isBlacklisted) {
							log.warn("JWT Filter: Token is blacklisted: jti={}", jti);
							return Mono.just(false);
						}
						log.debug("JWT Filter: Token is not blacklisted: jti={}", jti);

						log.info("JWT Filter: Token validation successful for jti={}", jti);
						return Mono.just(true);
					})
					.onErrorResume(e -> {
						log.error("JWT Filter: Token validation error: {}", e.getMessage(), e);
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
	 */
	public static class Config {
		private String skipPaths;

		public String getSkipPaths() {
			return skipPaths;
		}

		public void setSkipPaths(String skipPaths) {
			this.skipPaths = skipPaths;
		}

		/**
		 * Gets skip paths as a list.
		 */
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

