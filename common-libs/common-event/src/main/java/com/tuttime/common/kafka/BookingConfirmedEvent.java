package com.tuttime.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// Event published by other services for POS sync request
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingConfirmedEvent {
	private String bookingId;
	private String posSystem;
	private Instant timestamp;
}