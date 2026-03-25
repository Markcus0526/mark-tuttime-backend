package com.tuttime.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_status_histories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "history_id")
	private Long historyId;

	@Column(name = "booking_id", nullable = false, columnDefinition = "UUID")
	private UUID bookingId;

	@Column(name = "old_status", nullable = false, length = 20)
	private String oldStatus;

	@Column(name = "new_status", nullable = false, length = 20)
	private String newStatus;

	@Column(name = "changed_by", columnDefinition = "UUID")
	private UUID changedBy;

	@Column(name = "changed_at", nullable = false, updatable = false)
	private Instant changedAt;

	@Column(name = "reason", length = 255)
	private String reason;

	@PrePersist
	protected void onCreate() {
		changedAt = Instant.now();
	}

	// Getters and Setters omitted for brevity
}
