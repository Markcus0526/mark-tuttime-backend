package com.tuttime.restaurant.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@jakarta.persistence.Table(name = "branch_schedules", schema = "restaurant_service", indexes = {
		@Index(name = "idx_branch_schedule_branch_id", columnList = "branch_id"),
		@Index(name = "idx_branch_schedule_day_of_week", columnList = "day_of_week"),
		@Index(name = "idx_branch_schedule_is_deleted", columnList = "is_deleted")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSchedule {

	@Id
	@GeneratedValue
	@Column(name = "schedule_id", nullable = false, updatable = false)
	private UUID scheduleId;

	@Column(name = "branch_id", nullable = false)
	private UUID branchId;

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
	private OffsetDateTime deletedAt;

	@Column(name = "created_at", nullable = false, updatable = false)
	private OffsetDateTime createdAt;

	@Column(name = "updated_at")
	private OffsetDateTime updatedAt;

	@PrePersist
	public void prePersist() {
		updatedAt = createdAt;
	}

	@PreUpdate
	public void preUpdate() {
		updatedAt = Instant.now().atOffset(ZoneOffset.UTC);
	}
}