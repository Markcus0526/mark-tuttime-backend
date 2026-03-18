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

/** Fallback Controller for Circuit Breaker - provides fallback responses when services are unavailable. */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    /** Fallback endpoint for user service when circuit breaker is open. */
    @RequestMapping(value = "/user-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        return Mono.just(createFallbackResponse("User service is temporarily unavailable"));
    }

    /** Fallback endpoint for token service when circuit breaker is open. */
    @RequestMapping(value = "/token-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> tokenServiceFallback() {
        return Mono.just(createFallbackResponse("Token service is temporarily unavailable"));
    }

    /** Fallback endpoint for mining service when circuit breaker is open. */
    @RequestMapping(value = "/mining-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> miningServiceFallback() {
        return Mono.just(createFallbackResponse("Mining service is temporarily unavailable"));
    }

    /** Fallback endpoint for content service when circuit breaker is open. */
    @RequestMapping(value = "/content-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> contentServiceFallback() {
        return Mono.just(createFallbackResponse("Content service is temporarily unavailable"));
    }

    /** Fallback endpoint for conversion service when circuit breaker is open. */
    @RequestMapping(value = "/conversion-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> conversionServiceFallback() {
        return Mono.just(createFallbackResponse("Conversion service is temporarily unavailable"));
    }

    /** Fallback endpoint for rewards service when circuit breaker is open. */
    @RequestMapping(value = "/rewards-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> rewardsServiceFallback() {
        return Mono.just(createFallbackResponse("Rewards service is temporarily unavailable"));
    }

    /** Fallback endpoint for notification service when circuit breaker is open. */
    @RequestMapping(value = "/notification-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> notificationServiceFallback() {
        return Mono.just(createFallbackResponse("Notification service is temporarily unavailable"));
    }

    /** Fallback endpoint for booking integration service when circuit breaker is open. */
    @RequestMapping(value = "/booking-integration-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> bookingIntegrationServiceFallback() {
        return Mono.just(createFallbackResponse("Booking integration service is temporarily unavailable"));
    }

    /** Fallback endpoint for staking service when circuit breaker is open. */
    @RequestMapping(value = "/staking-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> stakingServiceFallback() {
        return Mono.just(createFallbackResponse("Staking service is temporarily unavailable"));
    }

    /** Fallback endpoint for messaging service when circuit breaker is open. */
    @RequestMapping(value = "/messaging-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> messagingServiceFallback() {
        return Mono.just(createFallbackResponse("Messaging service is temporarily unavailable"));
    }

    /** Fallback endpoint for social service when circuit breaker is open. */
    @RequestMapping(value = "/social-service", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public Mono<ResponseEntity<Map<String, Object>>> socialServiceFallback() {
        return Mono.just(createFallbackResponse("Social service is temporarily unavailable"));
    }

    /** Creates a standardized fallback response. */
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

