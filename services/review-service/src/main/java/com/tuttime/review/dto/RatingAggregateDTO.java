package com.tuttime.review.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingAggregateDTO {

	private UUID restaurantId;
	private BigDecimal averageRating;
	private int totalReviews;
	private Instant lastUpdated;
}
