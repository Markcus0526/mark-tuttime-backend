package com.tuttime.restaurant.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tuttime.common.entity.BaseDeleteEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "schedules", schema = "restaurant_service", indexes = {
		@Index(name = "idx_schedule_day_of_week", columnList = "day_of_week"),
		@Index(name = "idx_schedule_deleted", columnList = "deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Schedule extends BaseDeleteEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "schedule_id", nullable = false, updatable = false)
	private UUID scheduleId;

	@JoinColumn(name = "entity_id", nullable = false)
	private UUID entityId;

	@Column(name = "day_of_week", nullable = false)
	private Short dayOfWeek;

	@Column(name = "open_time", nullable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
	private LocalTime openTime;

	@Column(name = "close_time", nullable = false)
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm:ss")
	private LocalTime closeTime;

	@Builder.Default
	@Column(name = "closed", nullable = false)
	private Boolean closed = false;
}