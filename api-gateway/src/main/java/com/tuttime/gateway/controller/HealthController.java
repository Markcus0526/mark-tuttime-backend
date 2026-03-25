package com.tuttime.gateway.controller;

import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.Map;

/**
 * Health Check Controller: provides /health and /api/v1/health endpoints for Spring Cloud Gateway.
 */
@RestController
@RequestMapping
public class HealthController {

	private final HealthEndpoint healthEndpoint;

	public HealthController(HealthEndpoint healthEndpoint) {
		this.healthEndpoint = healthEndpoint;
	}

	/**
	 * Health check endpoint at /health
	 */
	@GetMapping("/health")
	public Mono<ResponseEntity<Map<String, Object>>> health() {
		return getHealthResponse();
	}

	/**
	 * Health check endpoint at /api/v1/health
	 */
	@GetMapping("/api/v1/health")
	public Mono<ResponseEntity<Map<String, Object>>> healthApi() {
		return getHealthResponse();
	}

	/**
	 * Gets the health response from the health endpoint.
	 * Uses subscribeOn to move blocking operation to a different thread.
	 *
	 * @return Mono containing ResponseEntity with health information
	 */
	private Mono<ResponseEntity<Map<String, Object>>> getHealthResponse() {
		return Mono.fromCallable(() -> {
					HealthComponent healthComponent = healthEndpoint.health();

					Map<String, Object> response = new HashMap<>();

					if (healthComponent instanceof org.springframework.boot.actuate.health.Health) {
						org.springframework.boot.actuate.health.Health health =
								(org.springframework.boot.actuate.health.Health) healthComponent;
						Status status = health.getStatus();
						response.put("status", status.getCode());
						response.put("details", health.getDetails());

						int httpStatus = status.equals(Status.UP) ? 200 : 503;
						return ResponseEntity.status(httpStatus).body(response);
					} else {
						// Fallback for other health component types
						response.put("status", "UP");
						response.put("details", healthComponent);
						return ResponseEntity.ok(response);
					}
				})
				.subscribeOn(Schedulers.boundedElastic()); // Move blocking operation to bounded elastic scheduler
	}
}

