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

/**
 * Brute-Force Protection Filter: checks if IP/phone is blocked before routing to authentication endpoints.
 */
@Slf4j
@Component
public class BruteForceProtectionFilter extends AbstractGatewayFilterFactory<BruteForceProtectionFilter.Config> {

	private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
	private static final String BLOCKED_IP_PREFIX = "bruteforce:blocked:ip:";
	private static final String BLOCKED_PHONE_PREFIX = "bruteforce:blocked:phone:";

	/**
	 * List of endpoints that require brute-force protection
	 */
	private static final List<String> PROTECTED_ENDPOINTS = Arrays.asList(
			"/api/v1/auth/login",
			"/api/v1/auth/register",
			"/api/v1/auth/refresh",
			"/api/v1/password/forgot"
	);

	/**
	 * Constructor for Brute-Force Protection Filter.
	 */
	public BruteForceProtectionFilter(ReactiveRedisTemplate<String, String> reactiveRedisTemplate) {
		super(Config.class);
		this.reactiveRedisTemplate = reactiveRedisTemplate;
	}

	/**
	 * Returns the name of this filter for use in route configuration.
	 *
	 * @return The filter name "BruteForceProtection"
	 */
	@Override
	public String name() {
		return "BruteForceProtection";
	}

	/**
	 * Applies the brute-force protection filter to the request.
	 *
	 * <p>This method:
	 * <ol>
	 *   <li>Checks if the path is a protected endpoint</li>
	 *   <li>Extracts IP address and phone number from request</li>
	 *   <li>Checks if IP or phone number is blocked</li>
	 *   <li>Returns 429 if blocked</li>
	 * </ol>
	 *
	 * @param config Filter configuration
	 * @return GatewayFilter that checks brute-force protection
	 */
	@Override
	public GatewayFilter apply(Config config) {
		return (exchange, chain) -> {
			ServerHttpRequest request = exchange.getRequest();
			String path = request.getURI().getPath();

			log.debug("Brute-Force Protection Filter: Checking path: {}", path);

			// Skip brute-force protection for non-protected endpoints
			if (!isProtectedEndpoint(path)) {
				log.debug("Brute-Force Protection Filter: Not a protected endpoint, skipping: {}", path);
				return chain.filter(exchange);
			}

			// Extract IP address and phone number
			String ip = extractIpAddress(request);
			String phoneNumber = extractPhoneNumber(request);

			// Check if blocked (reactive)
			return checkBlocked(ip, phoneNumber)
					.flatMap(isBlocked -> {
						if (isBlocked) {
							log.warn("Brute-Force Protection Filter: Request blocked - IP={}, phone={}, path={}",
									ip, phoneNumber, path);
							return onBlocked(exchange, ip, phoneNumber);
						}

						log.debug("Brute-Force Protection Filter: Request allowed - IP={}, phone={}", ip, phoneNumber);
						return chain.filter(exchange);
					})
					.onErrorResume(e -> {
						log.error("Brute-Force Protection Filter: Error checking block status: {}", e.getMessage());
						// On error, allow request to proceed (fail open for availability)
						return chain.filter(exchange);
					});
		};
	}

	/**
	 * Checks if the given path is a protected endpoint.
	 *
	 * @param path The request path to check
	 * @return true if the path is protected, false otherwise
	 */
	private boolean isProtectedEndpoint(String path) {
		return PROTECTED_ENDPOINTS.stream().anyMatch(path::startsWith);
	}

	/**
	 * Extracts IP address from the request.
	 *
	 * <p>Checks X-Forwarded-For header first (for proxy/load balancer),
	 * then falls back to remote address.
	 *
	 * @param request The HTTP request
	 * @return IP address as string, or null if not available
	 */
	private String extractIpAddress(ServerHttpRequest request) {
		// Check X-Forwarded-For header (for proxy/load balancer)
		String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
		if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
			// X-Forwarded-For can contain multiple IPs, take the first one
			String[] ips = xForwardedFor.split(",");
			return ips[0].trim();
		}

		// Check X-Real-IP header
		String xRealIp = request.getHeaders().getFirst("X-Real-IP");
		if (xRealIp != null && !xRealIp.isEmpty()) {
			return xRealIp.trim();
		}

		// Fallback to remote address
		if (request.getRemoteAddress() != null && request.getRemoteAddress().getAddress() != null) {
			return request.getRemoteAddress().getAddress().getHostAddress();
		}

		return "unknown";
	}

	/**
	 * Extracts phone number from the request body or headers.
	 *
	 * <p>Note: For reactive requests, extracting from body requires buffering.
	 * For now, we'll extract from headers if available, or leave it null.
	 * The actual phone number will be checked in the user-service.
	 *
	 * @param request The HTTP request
	 * @return Phone number as string, or null if not available
	 */
	private String extractPhoneNumber(ServerHttpRequest request) {
		// Try to get from custom header (if client sends it)
		String phoneNumber = request.getHeaders().getFirst("X-Phone-Number");
		if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
			return phoneNumber.trim();
		}

		// Phone number will be extracted in user-service from the request body
		// For now, return null - the user-service will use the phone from the user record
		return null;
	}

	/**
	 * Checks if an IP address or phone number is blocked (reactive).
	 *
	 * @param ip          IP address to check
	 * @param phoneNumber Phone number to check
	 * @return Mono that emits true if blocked, false otherwise
	 */
	private Mono<Boolean> checkBlocked(String ip, String phoneNumber) {
		// Check IP block
		Mono<Boolean> ipBlocked = checkIpBlocked(ip);

		// Check phone block (if phone number is available)
		if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
			Mono<Boolean> phoneBlocked = checkPhoneBlocked(phoneNumber);
			return Mono.zip(ipBlocked, phoneBlocked)
					.map(tuple -> tuple.getT1() || tuple.getT2());
		}

		return ipBlocked;
	}

	/**
	 * Checks if an IP address is blocked (reactive).
	 *
	 * @param ip IP address to check
	 * @return Mono that emits true if IP is blocked, false otherwise
	 */
	private Mono<Boolean> checkIpBlocked(String ip) {
		if (ip == null || ip.trim().isEmpty() || "unknown".equals(ip)) {
			return Mono.just(false);
		}

		String ipBlockKey = BLOCKED_IP_PREFIX + ip;
		return reactiveRedisTemplate.hasKey(ipBlockKey)
				.flatMap(exists -> {
					if (!exists) {
						return Mono.just(false);
					}

					// Check if block has expired
					return reactiveRedisTemplate.opsForValue().get(ipBlockKey)
							.map(blockUntilStr -> {
								try {
									long blockUntil = Long.parseLong(blockUntilStr);
									long now = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
									return blockUntil > now;
								} catch (NumberFormatException e) {
									log.warn("Invalid block timestamp for IP: {}", ip, e);
									return false;
								}
							})
							.defaultIfEmpty(false);
				})
				.defaultIfEmpty(false)
				.onErrorReturn(false); // Fail open on error
	}

	/**
	 * Checks if a phone number is blocked (reactive).
	 *
	 * @param phoneNumber Phone number to check
	 * @return Mono that emits true if phone number is blocked, false otherwise
	 */
	private Mono<Boolean> checkPhoneBlocked(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
			return Mono.just(false);
		}

		String phoneBlockKey = BLOCKED_PHONE_PREFIX + phoneNumber;
		return reactiveRedisTemplate.hasKey(phoneBlockKey)
				.flatMap(exists -> {
					if (!exists) {
						return Mono.just(false);
					}

					// Check if block has expired
					return reactiveRedisTemplate.opsForValue().get(phoneBlockKey)
							.map(blockUntilStr -> {
								try {
									long blockUntil = Long.parseLong(blockUntilStr);
									long now = System.currentTimeMillis() / 1000; // Unix timestamp in seconds
									return blockUntil > now;
								} catch (NumberFormatException e) {
									log.warn("Invalid block timestamp for phone: {}", phoneNumber, e);
									return false;
								}
							})
							.defaultIfEmpty(false);
				})
				.defaultIfEmpty(false)
				.onErrorReturn(false); // Fail open on error
	}

	/**
	 * Creates an error response when request is blocked.
	 *
	 * @param exchange    The server web exchange
	 * @param ip          IP address that was blocked
	 * @param phoneNumber Phone number that was blocked (may be null)
	 * @return Mono that completes when the error response is written
	 */
	private Mono<Void> onBlocked(ServerWebExchange exchange, String ip, String phoneNumber) {
		ServerHttpResponse response = exchange.getResponse();
		response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
		response.getHeaders().add(HttpHeaders.CONTENT_TYPE, "application/json");

		String message = "Too many failed login attempts. Your IP address or phone number has been temporarily blocked. Please try again later.";
		String body = String.format(
				"{\"error\":\"Too many failed attempts\",\"status\":429,\"message\":\"%s\"}", message);

		DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
		return response.writeWith(Mono.just(buffer));
	}

	/**
	 * Configuration class for Brute-Force Protection Filter.
	 *
	 * <p>Configuration properties can be added here if needed for future enhancements.
	 */
	public static class Config {
		// Configuration properties can be added here if needed
	}
}

