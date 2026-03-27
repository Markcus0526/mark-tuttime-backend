package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "restaurant_schedules", schema = "restaurant_service")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RestaurantSchedule {

	@Id
	@GeneratedValue(generator = "UUID")
	@GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
//	@Type(type = "pg-uuid")
	@Column(name = "schedule_id", updatable = false, nullable = false)
	private UUID scheduleId;

	@Column(name = "restaurant_id", nullable = false)
	private UUID restaurantId;

	@Column(name = "day_of_week", nullable = false)
	private int dayOfWeek; // 0=Sunday ... 6=Saturday

	@Column(name = "open_time", nullable = false)
	private LocalTime openTime;

	@Column(name = "close_time", nullable = false)
	private LocalTime closeTime;

	@Column(name = "is_closed", nullable = false)
	private boolean isClosed;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		createdAt = OffsetDateTime.now();
		updatedAt = createdAt;
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = OffsetDateTime.now();
	}
}
