package com.tuttime.analytics.controller;

import com.tuttime.analytics.dto.EventLogDto;
import com.tuttime.analytics.service.EventLogService;
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

@Tag(name = "Event Logs", description = "API for managing event logs")
@RestController
@RequestMapping("/api/v1/event-logs")
@Validated
public class EventLogController {

	private final EventLogService eventLogService;

	public EventLogController(EventLogService eventLogService) {
		this.eventLogService = eventLogService;
	}

	@Operation(summary = "Create a new event log")
	@PostMapping
	public ResponseEntity<EventLogDto> createEventLog(@Valid @RequestBody EventLogDto eventLogDto) {
		EventLogDto created = eventLogService.createEventLog(eventLogDto);
		return new ResponseEntity<>(created, HttpStatus.CREATED);
	}

	@Operation(summary = "Get event log by ID")
	@GetMapping("/{id}")
	public ResponseEntity<EventLogDto> getEventLogById(@PathVariable UUID id) {
		EventLogDto dto = eventLogService.getEventLogById(id);
		return ResponseEntity.ok(dto);
	}

	@Operation(summary = "Get event logs filtered by type and time range")
	@GetMapping
	public ResponseEntity<List<EventLogDto>> getEventLogsByTypeAndTimeRange(
			@RequestParam @NotBlank @Size(max = 100) String eventType,
			@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime start,
			@RequestParam @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime end) {

		List<EventLogDto> results = eventLogService.getEventLogsByTypeAndTimeRange(eventType, start, end);
		return ResponseEntity.ok(results);
	}

	@Operation(summary = "Delete event log by ID")
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteEventLogById(@PathVariable UUID id) {
		eventLogService.deleteEventLogById(id);
		return ResponseEntity.noContent().build();
	}
}

