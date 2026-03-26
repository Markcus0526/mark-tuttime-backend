package com.tuttime.analytics.controller;

import com.tuttime.analytics.dto.AggregatedMetricDto;
import com.tuttime.analytics.service.AggregatedMetricService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Tag(name = "Aggregated Metrics", description = "API for managing aggregated metrics")
@RestController
@RequestMapping("/api/v1/aggregated-metrics")
@Validated
public class AggregatedMetricController {

	private final AggregatedMetricService aggregatedMetricService;

	public AggregatedMetricController(AggregatedMetricService aggregatedMetricService) {
		this.aggregatedMetricService = aggregatedMetricService;
	}

	@Operation(summary = "Create a new aggregated metric")
	@PostMapping
	public ResponseEntity<AggregatedMetricDto> createAggregatedMetric(@Valid @RequestBody AggregatedMetricDto dto) {
		AggregatedMetricDto created = aggregatedMetricService.createAggregatedMetric(dto);
		return new ResponseEntity<>(created, HttpStatus.CREATED);
	}

	@Operation(summary = "Get aggregated metric by ID")
	@GetMapping("/{id}")
	public ResponseEntity<AggregatedMetricDto> getAggregatedMetricById(@PathVariable UUID id) {
		AggregatedMetricDto dto = aggregatedMetricService.getAggregatedMetricById(id);
		return ResponseEntity.ok(dto);
	}

	@Operation(summary = "Get metrics filtered by metric name and time range")
	@GetMapping("/by-name")
	public ResponseEntity<List<AggregatedMetricDto>> getMetricsByNameAndTimeRange(
			@RequestParam @NotBlank @Size(max = 100) String metricName,
			@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
			@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {

		List<AggregatedMetricDto> results = aggregatedMetricService.getMetricsByNameAndTimeRange(metricName, start, end);
		return ResponseEntity.ok(results);
	}

	@Operation(summary = "Get metrics filtered by restaurant ID and time range")
	@GetMapping("/by-restaurant")
	public ResponseEntity<List<AggregatedMetricDto>> getMetricsByRestaurantAndTimeRange(
			@RequestParam @NotNull UUID restaurantId,
			@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
			@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {

		List<AggregatedMetricDto> results = aggregatedMetricService.getMetricsByRestaurantAndTimeRange(restaurantId, start, end);
		return ResponseEntity.ok(results);
	}

	@Operation(summary = "Delete aggregated metric by ID")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteAggregatedMetricById(@PathVariable UUID id) {
		aggregatedMetricService.deleteAggregatedMetricById(id);
		return ResponseEntity.noContent().build();
	}
}
