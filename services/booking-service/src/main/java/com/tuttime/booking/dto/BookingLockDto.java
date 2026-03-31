package com.tuttime.booking.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingLockDto {
	private UUID lockId;
	private UUID bookingId;
	private UUID tableId;
	private Instant lockAcquiredAt;
	private Instant lockExpiresAt;

	// Getters and Setters
}
