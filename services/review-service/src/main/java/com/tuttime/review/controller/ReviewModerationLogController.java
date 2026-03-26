package com.tuttime.review.controller;

import com.tuttime.review.dto.ReviewModerationLogDto;
import com.tuttime.review.service.ReviewModerationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/moderation-logs")
@Validated
@Tag(name = "Review Moderation Logs", description = "Operations for review moderation logs")
public class ReviewModerationLogController {

	private final ReviewModerationLogService moderationLogService;

	public ReviewModerationLogController(ReviewModerationLogService moderationLogService) {
		this.moderationLogService = moderationLogService;
	}

	@Operation(summary = "Get moderation log by ID")
	@GetMapping("/{id}")
	public ResponseEntity<ReviewModerationLogDto> getById(
			@Parameter(description = "UUID of the moderation log", required = true)
			@PathVariable @NotNull UUID id) {
		return ResponseEntity.ok(moderationLogService.getById(id));
	}

	@Operation(summary = "Get moderation logs by review ID")
	@GetMapping("/review/{reviewId}")
	public ResponseEntity<List<ReviewModerationLogDto>> getByReviewId(
			@Parameter(description = "UUID of the review", required = true)
			@PathVariable @NotNull UUID reviewId) {
		return ResponseEntity.ok(moderationLogService.getByReviewId(reviewId));
	}

	@Operation(summary = "Get moderation logs by moderator ID")
	@GetMapping("/moderator/{moderatorId}")
	public ResponseEntity<List<ReviewModerationLogDto>> getByModeratorId(
			@Parameter(description = "UUID of the moderator", required = true)
			@PathVariable @NotNull UUID moderatorId) {
		return ResponseEntity.ok(moderationLogService.getByModeratorId(moderatorId));
	}

	@Operation(summary = "Create a new moderation log")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ReviewModerationLogDto create(
			@Parameter(description = "Moderation log data", required = true)
			@Valid @RequestBody ReviewModerationLogDto dto) {
		return moderationLogService.create(dto);
	}

	@Operation(summary = "Delete a moderation log by ID")
	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(
			@Parameter(description = "UUID of the moderation log to delete", required = true)
			@PathVariable @NotNull UUID id) {
		moderationLogService.delete(id);
	}
}