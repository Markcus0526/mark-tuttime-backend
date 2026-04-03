package com.tuttime.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.UUID;

// Swagger-ready Record for API
@Schema(description = "Details of a moderation action")
public record ModerationLogResponse(
		UUID id,
		String action,
		String reason,
		OffsetDateTime createdAt
) {
}