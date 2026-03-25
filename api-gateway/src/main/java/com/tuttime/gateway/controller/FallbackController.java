package com.tuttime.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Fallback Controller for Circuit Breaker - provides fallback responses when services are unavailable.
 */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

	/**
	 * Fallback endpoint for user service when circuit breaker is open.
	 */
	@RequestMapping(value = "/user-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
		return Mono.just(createFallbackResponse("User service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for restaurant service when circuit breaker is open.
	 */
	@RequestMapping(value = "/restaurant-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> restaurantServiceFallback() {
		return Mono.just(createFallbackResponse("Token service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for menu service when circuit breaker is open.
	 */
	@RequestMapping(value = "/menu-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> menuServiceFallback() {
		return Mono.just(createFallbackResponse("Mining service is temporarily unavailable"));
	}

	/**
	 * Creates a standardized fallback response.
	 */
	private ResponseEntity<Map<String, Object>> createFallbackResponse(String message) {
		Map<String, Object> response = new HashMap<>();
		response.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
		response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
		response.put("error", "Service Unavailable");
		response.put("message", message);
		response.put("path", "/fallback");
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
	}
}

