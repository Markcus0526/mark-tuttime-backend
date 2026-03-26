package com.tuttime.review.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import org.hibernate.annotations.GenericGenerator;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "reviews",
		indexes = {
				@Index(name = "idx_review_user_id", columnList = "user_id"),
				@Index(name = "idx_review_restaurant_id", columnList = "restaurant_id"),
				@Index(name = "idx_review_status", columnList = "status"),
				@Index(name = "idx_review_created_at", columnList = "created_at")
		})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name="UUID", strategy = "org.hibernate.id.UUIDGenerator")
	@Column(name = "id", updatable = false, nullable = false)
	private UUID id;

	@Column(name = "user_id", nullable = false)
	private UUID userId;  // Reference to User Service

	@Column(name = "restaurant_id", nullable = false)
	private UUID restaurantId;  // Reference to Restaurant Service

	@Column(nullable = false)
	private int rating;

	@Column(columnDefinition = "TEXT")
	private String comment;

	@Builder.Default
	@Column(nullable = false, length = 20)
	private String status = "PENDING";

	@Builder.Default
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Builder.Default
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	public void setRating(int rating) {
		if (rating < 1 || rating > 5) {
			throw new IllegalArgumentException("Rating must be between 1 and 5.");
		}
		this.rating = rating;
	}

	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}

}
