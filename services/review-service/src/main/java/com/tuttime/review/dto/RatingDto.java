package com.tuttime.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class RatingDto {

    // Swagger-ready Record for Frontend
    @Schema(description = "Aggregated rating statistics for a branch")
    public record RatingStatsResponse(
            @Schema(example = "4.50") BigDecimal averageRating,
            @Schema(example = "120") Integer totalReviews,
            @Schema(description = "Key-value pair of star level to count") Map<String, Integer> distribution
    ) {
    }

    @Schema(description = "Response containing aggregated rating statistics for a branch")
    @Builder
    public record RatingAggregateResponse(

            @Schema(description = "ID of the branch (branch)", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID branchId,

            @Schema(description = "Overall average rating (0.00 to 5.00)", example = "4.75")
            BigDecimal averageRating,

            @Schema(description = "Total number of approved reviews", example = "128")
            Integer totalReviews,

            @Schema(description = "Star distribution map (Star Level -> Count)",
                    example = "{\"1\": 2, \"2\": 0, \"3\": 10, \"4\": 45, \"5\": 71}")
            Map<String, Integer> ratingDistribution
    ) {
    }
}
