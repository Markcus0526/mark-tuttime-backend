package com.tuttime.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "rating_aggregate")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingAggregate {

	@Id
	@Column(name = "restaurant_id", nullable = false, updatable = false)
	private UUID restaurantId;

	@Builder.Default
	@Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
	private BigDecimal averageRating = BigDecimal.ZERO;

	@Builder.Default
	@Column(name = "total_reviews", nullable = false)
	private int totalReviews = 0;

	@Builder.Default
	@Column(name = "last_updated", nullable = false)
	private Instant lastUpdated = Instant.now();

	@PrePersist
	@PreUpdate
	protected void onUpdate() {
		lastUpdated = Instant.now();
	}
}
