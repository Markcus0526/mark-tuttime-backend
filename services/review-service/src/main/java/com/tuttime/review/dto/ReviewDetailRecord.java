package com.tuttime.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Full review details including user and restaurant context")
public record ReviewDetailRecord(
		@Schema(description = "Review ID", example = "uuid")
		UUID id,

		@Schema(description = "The reviewer's ID")
		UUID userId,

		@Schema(description = "The restaurant's ID")
		UUID restaurantId,

		@Schema(description = "Rating 1-5", example = "5")
		int rating,

		@Schema(description = "User's text", example = "Great!")
		String comment,

		@Schema(description = "Current status", allowableValues = {"PENDING", "APPROVED"})
		String status,

		@Schema(description = "Creation timestamp")
		OffsetDateTime createdAt,

		@Schema(description = "History of moderation actions")
		List<ModerationLogResponse> history
) {}