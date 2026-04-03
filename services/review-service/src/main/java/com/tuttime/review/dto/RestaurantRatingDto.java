package com.tuttime.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record RestaurantRatingDto(
		@Schema(description = "Restaurant ID") UUID restaurantId,
		@Schema(description = "Average star rating", example = "4.75") BigDecimal averageRating,
		@Schema(description = "Total number of approved reviews") int totalReviews,
		@Schema(description = "Count per star level") Map<String, Integer> ratingDistribution
) {}