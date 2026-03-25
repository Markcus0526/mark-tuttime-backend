package com.tuttime.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_locks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLock {

	@Id
	@Column(name = "lock_id", columnDefinition = "UUID")
	private UUID lockId;

	@Column(name = "booking_id", nullable = false, columnDefinition = "UUID")
	private UUID bookingId;

	@Column(name = "table_id", nullable = false, columnDefinition = "UUID")
	private UUID tableId;

	@Column(name = "lock_acquired_at", nullable = false)
	private Instant lockAcquiredAt;

	@Column(name = "lock_expires_at", nullable = false)
	private Instant lockExpiresAt;

	// Getters and Setters omitted for brevity
}
