package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "restaurant_schedules", schema = "restaurant_service", indexes = {
		@Index(name = "idx_restaurant_schedule_restaurant_id", columnList = "restaurant_id"),
		@Index(name = "idx_restaurant_schedule_day_of_week", columnList = "day_of_week"),
		@Index(name = "idx_restaurant_schedule_is_deleted", columnList = "is_deleted")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RestaurantSchedule {

	@Id
	@GeneratedValue
	@Column(name = "schedule_id", nullable = false, updatable = false)
	private UUID scheduleId;

	@Column(name = "restaurant_id", nullable = false)
	private UUID restaurantId;

	@Column(name = "day_of_week", nullable = false)
	private Short dayOfWeek;

	@Column(name = "open_time", nullable = false)
	private LocalTime openTime;

	@Column(name = "close_time", nullable = false)
	private LocalTime closeTime;

	@Builder.Default
	@Column(name = "is_closed", nullable = false)
	private boolean isClosed = false;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	@PrePersist
	public void prePersist() {
		createdAt = Instant.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = Instant.now();
	}
}