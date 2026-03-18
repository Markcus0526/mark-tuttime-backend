package com.tuttime.gateway.filter;

import com.aparttoken.gateway.config.RateLimitingConfig;
import com.aparttoken.gateway.util.JwtTokenUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Rate Limiting Filter: IP-based for unauthenticated endpoints, user-based for authenticated endpoints. */
@Slf4j
@Component
public class RateLimitingFilter extends AbstractGatewayFilterFactory<RateLimitingFilter.Config> {

    private final Map<String, Supplier<Bucket>> rateLimitConfigurations;
    private final LettuceBasedProxyManager<byte[]> proxyManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final Map<String, BucketConfiguration> bucketConfigurations = new HashMap<>();

    // IP-based rate limit thresholds
    @Value("${rate.limit.signin.capacity:5}")
    private int signinCapacity;

    @Value("${rate.limit.signin.refill.period.minutes:1}")
    private int signinRefillPeriodMinutes;

    @Value("${rate.limit.signup.capacity:3}")
    private int signupCapacity;

    @Value("${rate.limit.signup.refill.period.minutes:1}")
    private int signupRefillPeriodMinutes;

    @Value("${rate.limit.signup['send-sms'].capacity:3}")
    private int signupSendSmsCapacity;

    @Value("${rate.limit.signup['send-sms'].refill.period.hours:1}")
    private int signupSendSmsRefillPeriodHours;

    @Value("${rate.limit.signup['verify-sms'].capacity:10}")
    private int signupVerifySmsCapacity;

    @Value("${rate.limit.signup['verify-sms'].refill.period.hours:1}")
    private int signupVerifySmsRefillPeriodHours;

    @Value("${rate.limit.password.forgot.capacity:3}")
    private int passwordForgotCapacity;

    @Value("${rate.limit.password.forgot.refill.period.hours:1}")
    private int passwordForgotRefillPeriodHours;

    @Value("${rate.limit.password['verify-sms'].capacity:10}")
    private int passwordVerifySmsCapacity;

    @Value("${rate.limit.password['verify-sms'].refill.period.hours:1}")
    private int passwordVerifySmsRefillPeriodHours;

    // User-based rate limit thresholds
    @Value("${rate.limit.auth.refresh.capacity:10}")
    private int authRefreshCapacity;

    @Value("${rate.limit.auth.refresh.refill.period.minutes:1}")
    private int authRefreshRefillPeriodMinutes;

    @Value("${rate.limit.authenticated.capacity:500}")
    private int authenticatedCapacity;

    @Value("${rate.limit.authenticated.refill.period.minutes:1}")
    private int authenticatedRefillPeriodMinutes;

    public RateLimitingFilter(Map<String, Supplier<Bucket>> rateLimitConfigurations, 
                              LettuceBasedProxyManager<byte[]> proxyManager,
                              JwtTokenUtil jwtTokenUtil) {
        super(Config.class);
        this.rateLimitConfigurations = rateLimitConfigurations;
        this.proxyManager = proxyManager;
        this.jwtTokenUtil = jwtTokenUtil;
        initializeBucketConfigurations();
    }
    
    private void initializeBucketConfigurations() {
        // Extract configurations on-demand when needed
    }

    /**
     * Returns the filter name for route configuration.
     */
    @Override
    public String name() {
        return "RateLimiting";
    }

    /** Endpoints excluded from rate limiting */
    private static final List<String> EXCLUDED_ENDPOINTS = Arrays.asList(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs"
    );

    /** Applies rate limiting: checks exclusions, extracts client ID, validates rate limit, adds headers. */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            log.debug("Rate Limiting Filter: Checking path: {}", path);

            if (isExcludedEndpoint(path)) {
                log.debug("Rate Limiting Filter: Excluded endpoint, skipping rate limiting for: {}", path);
                return chain.filter(exchange);
            }

            Supplier<Bucket> bucketSupplier = RateLimitingConfig.getRateLimitForPath(path, rateLimitConfigurations);
            
            if (bucketSupplier == null) {
                if (RateLimitingConfig.requiresAuthenticatedRateLimit(path)) {
                    bucketSupplier = rateLimitConfigurations.get("authenticated");
                } else {
                    log.debug("Rate Limiting Filter: No rate limit configuration for: {}", path);
                    return chain.filter(exchange);
                }
            }

            String clientId = extractClientIdentifier(request, path);
            if (clientId == null || clientId.isEmpty()) {
                log.warn("Rate Limiting Filter: Could not extract client identifier for path: {}", path);
                return chain.filter(exchange);
            }

            BucketConfiguration bucketConfig = getBucketConfiguration(path, bucketSupplier);
            if (bucketConfig == null) {
                log.warn("Rate Limiting Filter: Could not get bucket configuration for path: {}", path);
                return chain.filter(exchange);
            }
            
            String redisKeyPrefix = getRedisKeyPrefix(path);
            String finalClientId = clientId;
            byte[] redisKey = (redisKeyPrefix + clientId).getBytes(StandardCharsets.UTF_8);
            
            return Mono.fromCallable(() -> {
                // Bucket4j operations are blocking - run on blocking scheduler
                Bucket bucket = proxyManager.builder().build(redisKey, () -> bucketConfig);
                boolean consumed = bucket.tryConsume(1);
                long availableTokens = bucket.getAvailableTokens();
                long capacity = bucketConfig.getBandwidths()[0].getCapacity();
                
                return new RateLimitResult(consumed, availableTokens, capacity);
            })
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(rateLimitResult -> {
                ServerHttpResponse response = exchange.getResponse();
                addRateLimitHeaders(response, rateLimitResult);
                
                if (!rateLimitResult.isAllowed()) {
                    log.warn("Rate Limiting Filter: Rate limit exceeded for path: {}, client: {}", path, finalClientId);
                    return onRateLimitExceeded(exchange, rateLimitResult);
                }
                
                log.debug("Rate Limiting Filter: Request allowed for path: {}, client: {}, remaining: {}", 
                        path, finalClientId, rateLimitResult.getRemaining());
                return chain.filter(exchange);
            })
            .onErrorResume(e -> {
                log.error("Rate Limiting Filter: Error checking rate limit for path: {}", path, e);
                return chain.filter(exchange);
            });
        };
    }

    /** Checks if the path is excluded from rate limiting. */
    private boolean isExcludedEndpoint(String path) {
        return EXCLUDED_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    /** Extracts client identifier: User ID from JWT for authenticated endpoints, IP address otherwise. */
    private String extractClientIdentifier(ServerHttpRequest request, String path) {
        if (RateLimitingConfig.requiresAuthenticatedRateLimit(path)) {
            // Extract User ID from JWT token
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    Long userId = jwtTokenUtil.getUserIdFromToken(token);
                    if (userId != null) {
                        return "user:" + userId;
                    }
                } catch (Exception e) {
                    log.debug("Rate Limiting Filter: Could not extract user ID from token: {}", e.getMessage());
                }
            }
            return extractIpAddress(request);
        } else {
            return extractIpAddress(request);
        }
    }

    /** Extracts IP address: checks X-Forwarded-For header first, then remote address. */
    private String extractIpAddress(ServerHttpRequest request) {
        // Check X-Forwarded-For header (for proxy/load balancer)
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String[] ips = xForwardedFor.split(",");
            return "ip:" + ips[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return "ip:" + xRealIp.trim();
        }
        
        if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
            return "ip:" + request.getRemoteAddress().getAddress().getHostAddress();
        }
        
        return "ip:unknown";
    }

    /** Adds rate limit headers to the response. */
    private void addRateLimitHeaders(ServerHttpResponse response, RateLimitResult rateLimitResult) {
        response.getHeaders().add("X-RateLimit-Limit", String.valueOf(rateLimitResult.getCapacity()));
        response.getHeaders().add("X-RateLimit-Remaining", String.valueOf(rateLimitResult.getRemaining()));
        long resetTime = System.currentTimeMillis() / 1000 + 60; // Approximate reset time
        response.getHeaders().add("X-RateLimit-Reset", String.valueOf(resetTime));
    }

    /** Creates error response when rate limit is exceeded. */
    private Mono<Void> onRateLimitExceeded(ServerWebExchange exchange, RateLimitResult rateLimitResult) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
        
        String body = String.format(
                "{\"error\":\"Rate limit exceeded\",\"status\":429,\"message\":\"Too many requests. Please try again later.\"}");
        
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    /** Gets bucket configuration for a given path. */
    private BucketConfiguration getBucketConfiguration(String path, @SuppressWarnings("unused") Supplier<Bucket> bucketSupplier) {
        if (bucketConfigurations.containsKey(path)) {
            return bucketConfigurations.get(path);
        }
        
        try {
            BucketConfiguration config = createBucketConfigurationForPath(path);
            if (config != null) {
                bucketConfigurations.put(path, config);
                return config;
            }
        } catch (Exception e) {
            log.warn("Rate Limiting Filter: Error creating bucket configuration for path: {}", path, e);
        }
        
        return null;
    }
    
    /** Creates bucket configuration for a given path based on rate limit rules. */
    private BucketConfiguration createBucketConfigurationForPath(String path) {
        if (path.startsWith("/api/v1/auth/signin")) {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(signinCapacity)
                    .refillIntervally(signinCapacity, Duration.ofMinutes(signinRefillPeriodMinutes))
                    .build();
            return BucketConfiguration.builder().addLimit(limit).build();
        } else if (path.startsWith("/api/v1/auth/signup")) {
            if (path.contains("/send-sms")) {
                Bandwidth limit = Bandwidth.builder()
                        .capacity(signupSendSmsCapacity)
                        .refillIntervally(signupSendSmsCapacity, Duration.ofHours(signupSendSmsRefillPeriodHours))
                        .build();
                return BucketConfiguration.builder().addLimit(limit).build();
            } else if (path.contains("/verify-sms")) {
                Bandwidth limit = Bandwidth.builder()
                        .capacity(signupVerifySmsCapacity)
                        .refillIntervally(signupVerifySmsCapacity, Duration.ofHours(signupVerifySmsRefillPeriodHours))
                        .build();
                return BucketConfiguration.builder().addLimit(limit).build();
            } else {
                Bandwidth limit = Bandwidth.builder()
                        .capacity(signupCapacity)
                        .refillIntervally(signupCapacity, Duration.ofMinutes(signupRefillPeriodMinutes))
                        .build();
                return BucketConfiguration.builder().addLimit(limit).build();
            }
        } else if (path.startsWith("/api/v1/password/forgot")) {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(passwordForgotCapacity)
                    .refillIntervally(passwordForgotCapacity, Duration.ofHours(passwordForgotRefillPeriodHours))
                    .build();
            return BucketConfiguration.builder().addLimit(limit).build();
        } else if (path.startsWith("/api/v1/password/verify-sms")) {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(passwordVerifySmsCapacity)
                    .refillIntervally(passwordVerifySmsCapacity, Duration.ofHours(passwordVerifySmsRefillPeriodHours))
                    .build();
            return BucketConfiguration.builder().addLimit(limit).build();
        } else if (path.startsWith("/api/v1/auth/refresh")) {
            Bandwidth limit = Bandwidth.builder()
                    .capacity(authRefreshCapacity)
                    .refillIntervally(authRefreshCapacity, Duration.ofMinutes(authRefreshRefillPeriodMinutes))
                    .build();
            return BucketConfiguration.builder().addLimit(limit).build();
        } else if (RateLimitingConfig.requiresAuthenticatedRateLimit(path)) {
            // Default for authenticated endpoints
            Bandwidth limit = Bandwidth.builder()
                    .capacity(authenticatedCapacity)
                    .refillIntervally(authenticatedCapacity, Duration.ofMinutes(authenticatedRefillPeriodMinutes))
                    .build();
            return BucketConfiguration.builder().addLimit(limit).build();
        }
        
        return null;
    }
    
    /** Gets Redis key prefix for a given path. */
    private String getRedisKeyPrefix(String path) {
        if (path.startsWith("/api/v1/auth/signin")) {
            return "rate-limit:signin:";
        } else if (path.startsWith("/api/v1/auth/signup")) {
            if (path.contains("/send-sms")) {
                return "rate-limit:signup-send-sms:";
            } else if (path.contains("/verify-sms")) {
                return "rate-limit:signup-verify-sms:";
            } else {
                return "rate-limit:signup:";
            }
        } else if (path.startsWith("/api/v1/password/forgot")) {
            return "rate-limit:password-forgot:";
        } else if (path.startsWith("/api/v1/password/verify-sms")) {
            return "rate-limit:password-verify-sms:";
        } else if (path.startsWith("/api/v1/auth/refresh")) {
            return "rate-limit:refresh:";
        } else if (RateLimitingConfig.requiresAuthenticatedRateLimit(path)) {
            return "rate-limit:authenticated:";
        }
        
        return "rate-limit:default:";
    }

    /** Configuration class for Rate Limiting Filter. */
    public static class Config {
    }

    /** Internal class to hold rate limit check result. */
    private static class RateLimitResult {
        private final boolean allowed;
        private final long remaining;
        private final long capacity;

        public RateLimitResult(boolean allowed, long remaining, long capacity) {
            this.allowed = allowed;
            this.remaining = remaining;
            this.capacity = capacity;
        }

        public boolean isAllowed() {
            return allowed;
        }

        public long getRemaining() {
            return remaining;
        }

        public long getCapacity() {
            return capacity;
        }
    }
}

