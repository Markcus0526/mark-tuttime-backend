package com.tuttime.booking.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingStatusHistoryDto {
	private Long historyId;
	private UUID bookingId;
	private String oldStatus;
	private String newStatus;
	private UUID changedBy;
	private Instant changedAt;
	private String reason;

	// Getters and Setters
}