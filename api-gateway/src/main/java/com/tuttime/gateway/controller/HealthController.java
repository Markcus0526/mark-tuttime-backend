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

@RestController
@RequestMapping
public class HealthController {

	private final HealthEndpoint healthEndpoint;

	public HealthController(HealthEndpoint healthEndpoint) {
		this.healthEndpoint = healthEndpoint;
	}

	@GetMapping("/health")
	public Mono<ResponseEntity<Map<String, Object>>> health() {
		return getHealthResponse();
	}

	@GetMapping("/api/v1/health")
	public Mono<ResponseEntity<Map<String, Object>>> healthApi() {
		return getHealthResponse();
	}

	private Mono<ResponseEntity<Map<String, Object>>> getHealthResponse() {
		return Mono.fromCallable(() -> {
					HealthComponent healthComponent = healthEndpoint.health();
					Status status = healthComponent.getStatus(); // Safe interface method

					Map<String, Object> response = new HashMap<>();
					response.put("status", status.getCode());
					response.put("details", healthComponent);

					int httpStatus = status.equals(Status.UP) ? 200 : 503;
					return ResponseEntity.status(httpStatus).body(response);
				})
				.subscribeOn(Schedulers.boundedElastic());
	}
}