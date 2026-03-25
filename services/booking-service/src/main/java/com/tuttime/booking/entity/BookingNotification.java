package com.tuttime.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingNotification {

	@Id
	@Column(name = "notification_id", columnDefinition = "UUID")
	private UUID notificationId;

	@Column(name = "booking_id", nullable = false, columnDefinition = "UUID")
	private UUID bookingId;

	@Column(name = "notification_type", nullable = false, length = 50)
	private String notificationType;

	@Column(name = "sent_at", nullable = false, updatable = false)
	private Instant sentAt;

	@Column(name = "status", nullable = false, length = 20)
	private String status;

	@Column(name = "recipient_contact", nullable = false, length = 255)
	private String recipientContact;

	@PrePersist
	protected void onCreate() {
		sentAt = Instant.now();
	}

	// Getters and Setters omitted for brevity
}
