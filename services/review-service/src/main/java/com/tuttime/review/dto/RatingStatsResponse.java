package com.tuttime.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Map;

// Swagger-ready Record for Frontend
@Schema(description = "Aggregated rating statistics for a restaurant")
public record RatingStatsResponse(
		@Schema(example = "4.50") BigDecimal averageRating,
		@Schema(example = "120") Integer totalReviews,
		@Schema(description = "Key-value pair of star level to count") Map<String, Integer> distribution
) {
}