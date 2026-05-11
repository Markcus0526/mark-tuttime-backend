package com.tuttime.gateway.filter;

import com.tuttime.gateway.service.TokenValidationService;
import com.tuttime.gateway.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ServerWebExchangeDecorator;
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
	private final long redisTimeoutMillis;

	public JwtAuthenticationFilter(
			JwtTokenUtil jwtTokenUtil,
			TokenValidationService tokenValidationService,
			@Value("${jwt.redis-timeout-ms:2000}") long redisTimeoutMillis) {
		super(Config.class);
		this.jwtTokenUtil = jwtTokenUtil;
		this.tokenValidationService = tokenValidationService;
		this.redisTimeoutMillis = redisTimeoutMillis;
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
			// Health & monitoring
			new PublicEndpointRule("/health", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/api/v1/health", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),
			new PublicEndpointRule("/actuator", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")),

			// OpenAPI spec documents (public)
//			new PublicEndpointRule("/user-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/restaurant-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/menu-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/booking-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/order-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/payment-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/pos-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/review-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/analytics-docs/v3/api-docs", List.of("GET")),
//			new PublicEndpointRule("/notification-docs/v3/api-docs", List.of("GET")),

			// Swagger UI assets
			new PublicEndpointRule("/swagger-ui", List.of("GET")),
			new PublicEndpointRule("/v3/api-docs", List.of("GET")),
			new PublicEndpointRule("/swagger-ui.html", List.of("GET")),
			new PublicEndpointRule("/webjars", List.of("GET")),

			// Media files
			new PublicEndpointRule("/user-medias/", List.of("GET")),
			new PublicEndpointRule("/restaurant-medias/", List.of("GET")),
			new PublicEndpointRule("/menu-medias/", List.of("GET")),

			// Auth endpoints
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

			// Public read-only endpoints
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
			if (exchange.getResponse().isCommitted()) {
				return chain.filter(exchange);
			}

			ServerHttpRequest request = exchange.getRequest();
			String path = request.getURI().getPath();
			String method = request.getMethod().name();
			log.debug("JWT Filter: path={}, method={}", path, method);

			if ("OPTIONS".equalsIgnoreCase(method)) {
				return chain.filter(exchange);
			}

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
			return validateTokenAndMutateExchange(token, exchange)
					.flatMap(modifiedExchange -> chain.filter(modifiedExchange))
					.switchIfEmpty(Mono.empty());
		};
	}

	private Mono<ServerWebExchange> validateTokenAndMutateExchange(String token, ServerWebExchange exchange) {
		try {
			UUID userId;
			String email;
			String role;
			// Extract user info from token (without Redis checks first, but we still need to validate)
			if (!jwtTokenUtil.validateToken(token)) {
				return onErrorMono(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
			}
			if (!jwtTokenUtil.validateIssuer(token) || !jwtTokenUtil.validateAudience(token)) {
				return onErrorMono(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
			}
			String jti = jwtTokenUtil.getJtiFromToken(token);
			if (jti == null) {
				return onErrorMono(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
			}

			userId = jwtTokenUtil.getUserIdFromToken(token);
			email = jwtTokenUtil.getEmailFromToken(token);
			role = jwtTokenUtil.getRoleFromToken(token);
			if (userId == null || email == null) {
				return onErrorMono(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
			}

			// Redis checks (fail‑closed)
			Duration timeout = Duration.ofMillis(redisTimeoutMillis);
			Mono<Boolean> whitelistCheck = tokenValidationService.isTokenWhitelisted(jti)
					.timeout(timeout).onErrorReturn(false);
			Mono<Boolean> blacklistCheck = tokenValidationService.isTokenBlacklisted(jti)
					.timeout(timeout).onErrorReturn(true);
			Mono<Boolean> userBlockCheck = tokenValidationService.isUserBlocked(userId)
					.timeout(timeout).onErrorReturn(true);

			return Mono.zip(whitelistCheck, blacklistCheck, userBlockCheck)
					.flatMap(tuple -> {
						boolean isWhitelisted = tuple.getT1();
						boolean isBlacklisted = tuple.getT2();
						boolean isUserBlocked = tuple.getT3();

						if (!isWhitelisted) {
							log.warn("JWT Filter: Token not whitelisted: jti={}", jti);
							return onErrorMono(exchange, "Token not whitelisted", HttpStatus.UNAUTHORIZED);
						}
						if (isBlacklisted) {
							log.warn("JWT Filter: Token blacklisted: jti={}", jti);
							return onErrorMono(exchange, "Token revoked", HttpStatus.UNAUTHORIZED);
						}
						if (isUserBlocked) {
							log.warn("JWT Filter: User blocked: userId={}", userId);
							return onErrorMono(exchange, "User account blocked", HttpStatus.FORBIDDEN);
						}

						// Build new exchange with added headers
						ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
								.header("X-User-Id", userId.toString())
								.header("X-User-Email", email)
								.header("X-User-Roles", role != null ? role : "")
								.build();
						return Mono.just(new ServerWebExchangeDecorator(exchange) {
							@Override
							public ServerHttpRequest getRequest() {
								return mutatedRequest;
							}
						});
					});
		} catch (Exception e) {
			log.error("JWT Filter: Token validation exception", e);
			return onErrorMono(exchange, "Authentication error", HttpStatus.INTERNAL_SERVER_ERROR);
		}
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
		if (method.equals("GET") && path.matches("^/api/v1/users/[^/]+/image.*")) {
			return true;
		}
		return false;
	}

	private Mono<ServerWebExchange> onErrorMono(ServerWebExchange exchange, String message, HttpStatus status) {
		return onError(exchange, message, status).then(Mono.empty());
	}

	private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
		ServerHttpResponse response = exchange.getResponse();
		if (response.isCommitted()) return Mono.empty();
		response.setStatusCode(status);
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
		String body = String.format("{\"error\":\"%s\",\"status\":%d}", message, status.value());
		return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8))));
	}

	public static class Config {
		private String skipPaths;
		public String getSkipPaths() { return skipPaths; }
		public void setSkipPaths(String skipPaths) { this.skipPaths = skipPaths; }
		public List<String> getSkipPathsList() {
			if (skipPaths == null || skipPaths.isBlank()) return List.of();
			return Arrays.stream(skipPaths.split(","))
					.map(String::trim)
					.filter(s -> !s.isEmpty())
					.toList();
		}
	}
}