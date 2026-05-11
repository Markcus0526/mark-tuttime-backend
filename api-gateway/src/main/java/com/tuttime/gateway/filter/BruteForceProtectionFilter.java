package com.tuttime.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class BruteForceProtectionFilter extends AbstractGatewayFilterFactory<BruteForceProtectionFilter.Config> {

	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
	private static final String BLOCKED_IP_PREFIX = "bruteforce:blocked:ip:";
	private static final String BLOCKED_PHONE_PREFIX = "bruteforce:blocked:phone:";

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

	public BruteForceProtectionFilter(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
		super(Config.class);
		this.reactiveRedisTemplate = reactiveRedisTemplate;
	}

	@Override
	public String name() {
		return "BruteForceProtection";
	}

	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			String path = request.getURI().getPath();

			if (!isProtectedEndpoint(path)) {
				return chain.filter(exchange);
			}

			String ip = extractIpAddress(request);
			String phone = extractPhoneNumber(request);

			return checkBlocked(ip, phone)
					.flatMap(blocked -> {
						if (blocked) {
							log.warn("Blocked request – IP={}, phone={}, path={}", ip, phone, path);
							return onBlocked(exchange);
						}
						return chain.filter(exchange);
					})
					.onErrorResume(e -> {
						log.error("Brute force check failed, allowing request (fail‑open): {}", e.getMessage());
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

	private String extractPhoneNumber(ServerHttpRequest request) {
		String phone = request.getHeaders().getFirst("X-Phone-Number");
		return (phone != null && !phone.trim().isEmpty()) ? phone.trim() : null;
	}

	private Mono<Boolean> checkBlocked(String ip, String phone) {
		Mono<Boolean> ipBlocked = isKeyPresent(BLOCKED_IP_PREFIX + ip);
		if (phone != null && !phone.isEmpty()) {
			Mono<Boolean> phoneBlocked = isKeyPresent(BLOCKED_PHONE_PREFIX + phone);
			return Mono.zip(ipBlocked, phoneBlocked)
					.map(tuple -> tuple.getT1() || tuple.getT2());
		}
		return ipBlocked;
	}

	private Mono<Boolean> isKeyPresent(String key) {
		return reactiveRedisTemplate.hasKey(key)
				.flatMap(exists -> {
					if (!exists) return Mono.just(false);
					return reactiveRedisTemplate.getExpire(key)
							.map(ttl -> ttl != null && ttl.getSeconds() > 0)
							.defaultIfEmpty(false);
				})
				.defaultIfEmpty(false)
				.onErrorReturn(false);
	}

	private Mono<Void> onBlocked(ServerWebExchange exchange) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");
		String body = "{\"error\":\"Too many failed attempts\",\"status\":429,\"message\":\"Your IP address or phone number has been temporarily blocked. Please try again later.\"}";
		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		return response.writeWith(Mono.just(buffer));
	}

	public static class Config {
		// No configuration needed for now
	}
}