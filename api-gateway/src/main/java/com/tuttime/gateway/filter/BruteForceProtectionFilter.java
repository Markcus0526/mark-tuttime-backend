package com.tuttime.gateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
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
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class BruteForceProtectionFilter extends AbstractGatewayFilterFactory<BruteForceProtectionFilter.Config> {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final Duration redisTimeout;
    private final int ipThreshold;
    private final Duration ipBlockDuration;

    private static final String BLOCKED_IP_PREFIX = "bruteforce:blocked:ip:";
    private static final String ATTEMPTS_IP_PREFIX = "bruteforce:attempts:ip:";

    private static final List<String> PROTECTED_ENDPOINTS = Arrays.asList(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/password-reset/request",
            "/api/v1/auth/password-reset/confirm",
            "/api/v1/auth/otp/request",
            "/api/v1/auth/otp/verify",
            "/api/v1/auth/oauth-login"
    );

    public BruteForceProtectionFilter(
            ReactiveRedisTemplate<String, String> reactiveRedisTemplate,
            @Value("${jwt.redis-timeout-ms:2000}") long redisTimeoutMillis,
            @Value("${bruteforce.ip-block-threshold:5}") int ipThreshold,
            @Value("${bruteforce.ip-block-duration-minutes:15}") int ipBlockMinutes) {
        super(Config.class);
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.redisTimeout = Duration.ofMillis(redisTimeoutMillis);
        this.ipThreshold = ipThreshold;
        this.ipBlockDuration = Duration.ofMinutes(ipBlockMinutes);
    }

    @Override
    public String name() {
        return "BruteForceProtection";
    }

    @Data
    public static class Config {
        // No extra configuration attributes required
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();
            String method = request.getMethod().name();

            if (!isProtectedEndpoint(path) || "OPTIONS".equalsIgnoreCase(method)) {
                return chain.filter(exchange);
            }

            String ip = extractIpAddress(request);
            String blockKey = BLOCKED_IP_PREFIX + ip;
            String attemptsKey = ATTEMPTS_IP_PREFIX + ip;

            // 1. 🛑 RESILIENT HIGH-SPEED CHECK WITH TIMEOUT
            return reactiveRedisTemplate.hasKey(blockKey)
                    .timeout(redisTimeout) // 🛑 STABILITY FIX 1: Prevent gateway pool freezes if Redis hangs
                    .flatMap(isBlocked -> {
                        if (Boolean.TRUE.equals(isBlocked)) {
                            log.warn("BruteForce Blocked: IP {} attempted access to secure path {}", ip, path);
                            return onBlocked(exchange);
                        }

                        // 2. 🛑 MONITOR DOWNSTREAM RESPONSE UN-AUTHORIZED HANDSHAKES TO COUNT FAILURES
                        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                            HttpStatus statusCode = (HttpStatus) exchange.getResponse().getStatusCode();
                            if (statusCode == HttpStatus.UNAUTHORIZED) {
                                log.info("Un-authorized login event registered from IP: {}", ip);

                                // Increment failure outbox counters asynchronously off the main execution thread
                                reactiveRedisTemplate.opsForValue().increment(attemptsKey)
                                        .flatMap(attempts -> {
                                            if (attempts == 1) {
                                                return reactiveRedisTemplate.expire(attemptsKey, Duration.ofHours(1));
                                            }
                                            if (attempts >= ipThreshold) {
                                                log.error("SECURITY ALERT: IP {} exceeded login thresholds. Enforcing a {}-minute lock.", ip, ipBlockDuration.toMinutes());
                                                return reactiveRedisTemplate.opsForValue().set(blockKey, "BLOCKED", ipBlockDuration);
                                            }
                                            return Mono.just(true);
                                        }).subscribe();
                            }
                        }));
                    })
                    .onErrorResume(e -> {
                        // 🛑 RESILIENCY FIX 2: True Fail-Open capability ensures service availability during cloud failovers
                        log.error("Brute force protection subsystem unreachable. Bypassing check for stability: {}", e.getMessage());
                        return chain.filter(exchange);
                    });
        };
    }

    private boolean isProtectedEndpoint(String path) {
        return PROTECTED_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private String extractIpAddress(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp.trim();
        }
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return request.getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private Mono<Void> onBlocked(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format(
                "{\"error\":\"Too many failed attempts\",\"status\":429,\"message\":\"Your network address has been temporarily blocked for %d minutes due to unusual login failures.\"}",
                ipBlockDuration.toMinutes()
        );

        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
