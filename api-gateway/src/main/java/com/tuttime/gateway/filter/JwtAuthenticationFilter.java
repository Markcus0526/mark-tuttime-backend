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

	private record PublicEndpointRule(String pathPrefix, List<String> allowedMethods) {
		boolean matches(String path, String method) {
			return path.startsWith(pathPrefix) && allowedMethods.contains(method);
		}
	}

	private static final List<PublicEndpointRule> PUBLIC_ENDPOINT_RULES = Arrays.asList(
			new PublicEndpointRule("/health", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/api/v1/health", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/actuator", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/user-medias/", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/restaurant-medias/", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/menu-medias/", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/api/v1/auth/device-token", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/login", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/logout", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/oauth-login", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/otp/request", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/otp/verify", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/password-reset/confirm", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/password-reset/request", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/refresh-token", List.of("POST")),
			new PublicEndpointRule("/api/v1/auth/register", List.of("POST")),
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
			new PublicEndpointRule("/api/v1/menus/", List.of("GET")),
			new PublicEndpointRule("/api/v1/menus/branches/", List.of("GET")),
			new PublicEndpointRule("/api/v1/menu-items/", List.of("GET")),
			new PublicEndpointRule("/api/v1/menu-items/categories/", List.of("GET")),
			new PublicEndpointRule("/api/v1/menu-items/tags", List.of("GET"))
	);

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			// If the response is already committed, do nothing
			if (exchange.getResponse().isCommitted()) {
				log.debug("Response already committed, skipping JWT filter");
				return chain.filter(exchange);
			}

			ServerHttpRequest request = exchange.getRequest();
			String path = request.getURI().getPath();
			String method = request.getMethod().name();

			log.debug("JWT Filter: Checking path: {}, method: {}", path, method);

			// OPTIONS (CORS preflight) – skip
			if ("OPTIONS".equalsIgnoreCase(method)) {
				return chain.filter(exchange);
			}

			// Public endpoints – skip authentication
			List<String> skipPaths = config.getSkipPathsList();
			if (isPublicEndpoint(path, method, skipPaths)) {
				log.debug("JWT Filter: Public endpoint, skipping auth");
				return chain.filter(exchange);
			}

			log.debug("JWT Filter: Protected endpoint: {}", path);

			String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
			if (authHeader == null || !authHeader.startsWith("Bearer ")) {
				return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
			}

			String token = authHeader.substring(7);

			// Validate token – authentication errors only, do not catch downstream routing errors
			return validateTokenReactive(token)
					.flatMap(isValid -> {
						if (!isValid) {
							return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
						}
						try {
							UUID userId = jwtTokenUtil.getUserIdFromToken(token);
							String email = jwtTokenUtil.getEmailFromToken(token);
							String role = jwtTokenUtil.getRoleFromToken(token);

							if (userId == null || email == null) {
								return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
							}

							ServerHttpRequest modifiedRequest = request.mutate()
									.header("X-User-Id", userId.toString())
									.header("X-User-Email", email)
									.header("X-User-Role", role != null ? role : "")
									.build();

							log.debug("JWT Filter: Validated user: {} ({})", userId, email);
							return chain.filter(exchange.mutate().request(modifiedRequest).build());
						} catch (Exception e) {
							log.error("JWT Filter: Error extracting user info: {}", e.getMessage(), e);
							return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
						}
					});
			// ❗ No outer onErrorResume – downstream errors (503, etc.) are not intercepted
		};
	}

	private boolean isPublicEndpoint(String path, String method, List<String> skipPaths) {
		if (skipPaths != null && !skipPaths.isEmpty()) {
			for (String skipPath : skipPaths) {
				if (path.startsWith(skipPath.replace("*", ""))) {
					return true;
				}
			}
		}
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

	private Mono<Boolean> validateTokenReactive(String token) {
		try {
			String jti = jwtTokenUtil.getJtiFromToken(token);
			if (jti == null) {
				log.warn("JWT Filter: Token missing JTI claim");
				return Mono.just(false);
			}

			// Local validation
			if (!jwtTokenUtil.validateIssuer(token)) {
				log.warn("JWT Filter: Invalid issuer");
				return Mono.just(false);
			}
			if (!jwtTokenUtil.validateAudience(token)) {
				log.warn("JWT Filter: Invalid audience");
				return Mono.just(false);
			}
			if (!jwtTokenUtil.validateToken(token)) {
				log.warn("JWT Filter: Invalid signature/expiration");
				return Mono.just(false);
			}

			// Redis checks (fail‑closed)
			Mono<Boolean> whitelistCheck = tokenValidationService.isTokenWhitelisted(jti)
					.timeout(Duration.ofMillis(2000))
					.doOnError(e -> log.warn("Whitelist check error: jti={}, error={}", jti, e.getMessage()))
					.onErrorReturn(false);

			Mono<Boolean> blacklistCheck = tokenValidationService.isTokenBlacklisted(token)
					.timeout(Duration.ofMillis(2000))
					.doOnError(e -> log.warn("Blacklist check error: jti={}, assuming blacklisted", jti))
					.onErrorReturn(true);

			return Mono.zip(whitelistCheck, blacklistCheck)
					.flatMap(tuple -> {
						Boolean isWhitelisted = tuple.getT1();
						Boolean isBlacklisted = tuple.getT2();

						// TODO: Uncomment after implementing whitelist storage in user-service
						// if (!isWhitelisted) {
						//     log.warn("JWT Filter: Token not whitelisted: jti={}", jti);
						//     return Mono.just(false);
						// }
						// if (isBlacklisted) {
						//     log.warn("JWT Filter: Token is blacklisted: jti={}", jti);
						//     return Mono.just(false);
						// }

						log.debug("JWT Filter: Token validation successful for jti={}", jti);
						return Mono.just(true);
					});
		} catch (Exception e) {
			log.error("JWT Filter: Token validation exception: {}", e.getMessage(), e);
			return Mono.just(false);
		}
	}

	private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
		ServerHttpResponse response = exchange.getResponse();
		if (response.isCommitted()) {
			log.debug("Response already committed, cannot write error: {}", message);
			return Mono.empty();
		}
		response.setStatusCode(status);
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
		String body = String.format("{\"error\":\"%s\",\"status\":%d}", message, status.value());
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
	}

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