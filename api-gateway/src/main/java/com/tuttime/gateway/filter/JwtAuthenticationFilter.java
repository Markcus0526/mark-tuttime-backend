package com.tuttime.gateway.filter;

import com.tuttime.gateway.service.TokenValidationService;
import com.tuttime.gateway.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
@Slf4j
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

    // ----- Public endpoint rules (exact path prefixes) -----
    private record PublicEndpointRule(String pathPrefix, List<String> allowedMethods) {
        boolean matches(String path, String method) {
            return path.startsWith(pathPrefix) && allowedMethods.contains(method);
        }
    }

    // ----- Regex patterns for public endpoints (supports any service prefix like /user-docs, /restaurant-docs) -----
    private record PublicEndpointRegexRule(Pattern pattern, List<String> allowedMethods) {
        boolean matches(String path, String method) {
            return pattern.matcher(path).matches() && allowedMethods.contains(method);
        }
    }

    private static final List<PublicEndpointRule> PUBLIC_ENDPOINT_RULES = new ArrayList<>();
    private static final List<PublicEndpointRegexRule> PUBLIC_ENDPOINT_REGEX_RULES = new ArrayList<>();

    static {
        // -----------------------------------------------------------------
        // 1. Exact path rules (for direct API calls, e.g., Postman)
        // -----------------------------------------------------------------
        // Health & monitoring
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/health", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/health", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/actuator", List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")));

        // Swagger UI static assets
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/swagger-ui", List.of("GET")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/swagger-ui.html", List.of("GET")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/v3/api-docs", List.of("GET")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/webjars", List.of("GET")));

        // Media files
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/user-medias/", List.of("GET")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/restaurant-medias/", List.of("GET")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/menu-medias/", List.of("GET")));

        // Auth endpoints (direct)
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/device-token", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/login", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/logout", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/oauth-login", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/otp/request", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/otp/verify", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/password-reset/confirm", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/password-reset/request", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/refresh-token", List.of("POST")));
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/auth/register", List.of("POST")));

        // Public read-only endpoints (direct)
        PUBLIC_ENDPOINT_RULES.add(new PublicEndpointRule("/api/v1/payments/webhook", List.of("POST")));

        // OpenAPI spec documents
        PUBLIC_ENDPOINT_REGEX_RULES.add(new PublicEndpointRegexRule(Pattern.compile("^/(user|payment)-docs/v3/api-docs$"), List.of("GET")));
        // Auth endpoints (POST)
        PUBLIC_ENDPOINT_REGEX_RULES.add(new PublicEndpointRegexRule(Pattern.compile("^/(user|payment)-docs/api/v1/auth/.*"), List.of("POST")));
        // Swagger UI assets
        PUBLIC_ENDPOINT_REGEX_RULES.add(new PublicEndpointRegexRule(Pattern.compile("^/(user|payment)-docs/(swagger-ui|webjars|v3/api-docs).*"), List.of("GET")));
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (exchange.getResponse().isCommitted()) {
                return chain.filter(exchange);
            }

            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            // HARD SHORT-CIRCUIT FOR MEDIA PAYLOADS
            if ("GET".equalsIgnoreCase(method) && (
                    path.contains("/user-medias/") ||
                            path.contains("/restaurant-medias/") ||
                            path.contains("/menu-medias/"))) {
                return chain.filter(exchange);
            }

            if ("OPTIONS".equalsIgnoreCase(method)) {
                return chain.filter(exchange);
            }

            List<String> skipPaths = config.getSkipPathsList();
            if (isPublicEndpoint(path, method, skipPaths)) {
                // 🛑 SECURITY FIX 2: Strip user context headers from unauthenticated public traffic
                ServerHttpRequest cleanedRequest = request.mutate()
                        .headers(h -> {
                            h.remove("X-User-Id");
                            h.remove("X-User-Roles");
                        }).build();
                return chain.filter(exchange.mutate().request(cleanedRequest).build());
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            String token = authHeader.substring(7);

            // Execute the pipeline cleanly, short-circuiting the stream immediately upon error rejections
            return validateTokenAndMutateExchange(token, exchange)
                    .flatMap(modifiedExchange -> chain.filter(modifiedExchange))
                    // Clean escape route if a downstream error signal or empty signal is encountered
                    .onErrorResume(ex -> {
                        log.error("Authentication filter pipeline failed", ex);
                        return onError(exchange, "Authentication subsystem error", HttpStatus.INTERNAL_SERVER_ERROR);
                    });
        };
    }

    private Mono<ServerWebExchange> validateTokenAndMutateExchange(String token, ServerWebExchange exchange) {
        try {
            // 1. Basic JWT validation
            if (!jwtTokenUtil.validateToken(token)) {
                return Mono.defer(() -> onError(exchange, "Invalid token (validateToken)", HttpStatus.UNAUTHORIZED).then(Mono.empty()));
            }
            if (!jwtTokenUtil.validateIssuer(token) || !jwtTokenUtil.validateAudience(token)) {
                return Mono.defer(() -> onError(exchange, "Invalid token (validateIssuer)", HttpStatus.UNAUTHORIZED).then(Mono.empty()));
            }
            String jti = jwtTokenUtil.getJtiFromToken(token);
            if (jti == null) {
                return Mono.defer(() -> onError(exchange, "Invalid token (getJtiFromToken)", HttpStatus.UNAUTHORIZED).then(Mono.empty()));
            }

            // 2. Extract user info
            UUID userId = jwtTokenUtil.getUserIdFromToken(token);
            String role = jwtTokenUtil.getRoleFromToken(token);
            if (userId == null || role == null) {
                return Mono.defer(() -> onError(exchange, "Invalid token (userId, role)", HttpStatus.UNAUTHORIZED).then(Mono.empty()));
            }

            // 3. Redis checks (With strict, absolute Fail-Closed error handling)
            Duration timeout = Duration.ofMillis(redisTimeoutMillis);

            Mono<Boolean> whitelistCheck = tokenValidationService.isTokenWhitelisted(jti).timeout(timeout);
            Mono<Boolean> blacklistCheck = tokenValidationService.isTokenBlacklisted(jti).timeout(timeout);
            Mono<Boolean> userBlockCheck = tokenValidationService.isUserBlocked(userId).timeout(timeout);

            return Mono.zip(whitelistCheck, blacklistCheck, userBlockCheck)
                    .flatMap(tuple -> {
                        boolean isWhitelisted = tuple.getT1();
                        boolean isBlacklisted = tuple.getT2();
                        boolean isUserBlocked = tuple.getT3();

                        if (!isWhitelisted) {
                            log.warn("Token not whitelisted: jti={}", jti);
                            return onError(exchange, "Token session not recognized", HttpStatus.UNAUTHORIZED).then(Mono.empty());
                        }
                        if (isBlacklisted) {
                            log.warn("Token blacklisted: jti={}", jti);
                            return onError(exchange, "Token credentials revoked", HttpStatus.UNAUTHORIZED).then(Mono.empty());
                        }
                        if (isUserBlocked) {
                            log.warn("User blocked: userId={}", userId);
                            return onError(exchange, "User account blocked", HttpStatus.FORBIDDEN).then(Mono.empty());
                        }

                        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                                .header("X-User-Id", userId.toString())
                                .header("X-User-Roles", role)
                                .build();

                        return Mono.just(exchange.mutate().request(mutatedRequest).build());
                    })
                    .onErrorResume(ex -> {
                        log.error("Security Engine Outage: Blocked secure route access due to subsystem failure.", ex);
                        return onError(exchange, "Security subsystem currently unavailable", HttpStatus.SERVICE_UNAVAILABLE).then(Mono.empty());
                    });
        } catch (Exception e) {
            log.error("Token validation exception", e);
            return onError(exchange, "Authentication subsystem error", HttpStatus.INTERNAL_SERVER_ERROR).then(Mono.empty());
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
        for (PublicEndpointRegexRule rule : PUBLIC_ENDPOINT_REGEX_RULES) {
            if (rule.matches(path, method)) {
                return true;
            }
        }
        // Dynamic rule: user public images
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
        if (response.isCommitted())
            return Mono.empty();

        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"error\":\"%s\",\"status\":%d}", message, status.value());

        return response.writeWith(
                Mono.just(response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8)))
        );
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
            if (skipPaths == null || skipPaths.isBlank()) return List.of();
            return Arrays.stream(skipPaths.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }
}