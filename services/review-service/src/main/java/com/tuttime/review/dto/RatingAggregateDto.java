package com.tuttime.review.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingAggregateDto {

	private UUID restaurantId;
	private BigDecimal averageRating;
	private int totalReviews;
	private Instant lastUpdated;
}
