package com.tuttime.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Review output payload")
public record ReviewResponse(
		@Schema(example = "uuid")
		UUID id,

		@Schema(example = "uuid")
		UUID userId,

		@Schema(example = "5")
		int rating,

		@Schema(example = "Great food!")
		String comment,

		@Schema(example = "APPROVED")
		String status,

		@Schema(description = "ISO-8601 Timestamp")
		String createdAt
) {
}