package com.tuttime.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "restaurant_schedules", schema = "restaurant_service", indexes = {
		@Index(name = "idx_restaurant_schedule_restaurant_id", columnList = "restaurant_id"),
		@Index(name = "idx_restaurant_schedule_day_of_week", columnList = "day_of_week"),
		@Index(name = "idx_restaurant_schedule_is_deleted", columnList = "is_deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RestaurantSchedule extends BaseTimeStampEntity {

	@Id
	@GeneratedValue
	@Column(name = "schedule_id", nullable = false, updatable = false)
	private UUID scheduleId;

	@Column(name = "restaurant_id", nullable = false)
	private UUID restaurantId;

	@Column(name = "day_of_week", nullable = false)
	private Short dayOfWeek;

	@Column(name = "open_time", nullable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
	private LocalTime openTime;

	@Column(name = "close_time", nullable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
	private LocalTime closeTime;

	@Builder.Default
	@Column(name = "is_closed", nullable = false)
	private boolean isClosed = false;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean isDeleted = false;

	@Column(name = "deleted_at")
	private OffsetDateTime deletedAt;
}