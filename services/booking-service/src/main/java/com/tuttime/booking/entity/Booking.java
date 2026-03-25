package com.tuttime.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

	@Id
	@Column(name = "booking_id", columnDefinition = "UUID")
	private UUID bookingId;

	@Column(name = "user_id", nullable = false, columnDefinition = "UUID")
	private UUID userId;

	@Column(name = "restaurant_id", nullable = false, columnDefinition = "UUID")
	private UUID restaurantId;

	@Column(name = "branch_id", nullable = false, columnDefinition = "UUID")
	private UUID branchId;

	@Column(name = "zone_id", nullable = false, columnDefinition = "UUID")
	private UUID zoneId;

	@Column(name = "table_id", nullable = false, columnDefinition = "UUID")
	private UUID tableId;

	@Column(name = "booking_status", nullable = false, length = 20)
	private String bookingStatus;

	@Column(name = "booking_datetime", nullable = false)
	private Instant bookingDatetime;

	@Column(name = "party_size", nullable = false)
	private int partySize;

	@Column(name = "lead_time_minutes")
	private Integer leadTimeMinutes;

	@Column(name = "grace_period_mins")
	private Integer gracePeriodMins;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	@Column(name = "cancellation_reason", length = 255)
	private String cancellationReason;

	@Column(name = "auto_cancelled")
	private Boolean autoCancelled;

	@PrePersist
	protected void onCreate() {
		createdAt = Instant.now();
	}

	@PreUpdate
	protected void onUpdate() {
		updatedAt = Instant.now();
	}

}
