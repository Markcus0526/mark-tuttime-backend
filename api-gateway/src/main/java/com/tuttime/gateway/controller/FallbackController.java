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
		return Mono.just(createFallbackResponse("Restaurant service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for menu service when circuit breaker is open.
	 */
	@RequestMapping(value = "/menu-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> menuServiceFallback() {
		return Mono.just(createFallbackResponse("Menu service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for booking service when circuit breaker is open.
	 */
	@RequestMapping(value = "/booking-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> bookingServiceFallback() {
		return Mono.just(createFallbackResponse("Booking service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for payment service when circuit breaker is open.
	 */
	@RequestMapping(value = "/payment-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> paymentServiceFallback() {
		return Mono.just(createFallbackResponse("Payment service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for pos service when circuit breaker is open.
	 */
	@RequestMapping(value = "/pos-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> posServiceFallback() {
		return Mono.just(createFallbackResponse("POS service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for order service when circuit breaker is open.
	 */
	@RequestMapping(value = "/order-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> orderServiceFallback() {
		return Mono.just(createFallbackResponse("Order service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for review service when circuit breaker is open.
	 */
	@RequestMapping(value = "/review-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> reviewServiceFallback() {
		return Mono.just(createFallbackResponse("Review service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for analytics service when circuit breaker is open.
	 */
	@RequestMapping(value = "/analytics-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> analyticsServiceFallback() {
		return Mono.just(createFallbackResponse("Analytics service is temporarily unavailable"));
	}

	/**
	 * Fallback endpoint for notification service when circuit breaker is open.
	 */
	@RequestMapping(value = "/notification-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
	public Mono<ResponseEntity<Map<String, Object>>> notificationServiceFallback() {
		return Mono.just(createFallbackResponse("Notification service is temporarily unavailable"));
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

