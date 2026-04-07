package com.tuttime.restaurant.entity;

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
@jakarta.persistence.Table(name = "branch_schedules", schema = "restaurant_service", indexes = {
		@Index(name = "idx_branch_schedule_branch_id", columnList = "branch_id"),
		@Index(name = "idx_branch_schedule_day_of_week", columnList = "day_of_week"),
		@Index(name = "idx_branch_schedule_is_deleted", columnList = "is_deleted")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class BranchSchedule extends BaseTimeStampEntity {

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
}