package com.tuttime.booking.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingNotificationDto {
	private UUID notificationId;
	private UUID bookingId;
	private String notificationType;
	private Instant sentAt;
	private String status;
	private String recipientContact;

	// Getters and Setters
}
