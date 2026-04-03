package com.tuttime.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@Schema(description = "Request body for creating a new review")
public record ReviewRequest(
		@NotNull @Schema(description = "Target restaurant ID")
		UUID restaurantId,

		@Min(1) @Max(5)
		@Schema(description = "Rating score", example = "5")
		int rating,

		@Size(max = 1000)
		@Schema(description = "Optional review text")
		String comment
) {
}