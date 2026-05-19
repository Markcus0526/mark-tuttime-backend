package com.tuttime.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
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

    @RequestMapping("/fallback/{service}")
    public Mono<ResponseEntity<Map<String, Object>>> fallback(@PathVariable String service) {
        return Mono.just(createFallbackResponse(service + " service is temporarily unavailable"));
    }

    @RequestMapping("/404")
    public Mono<Map<String, Object>> notFound(ServerWebExchange exchange) {
        return Mono.just(Map.of(
                "timestamp", OffsetDateTime.now(),
                "status", 404,
                "error", "Not Found",
                "path", exchange.getRequest().getURI().getPath()
        ));
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

