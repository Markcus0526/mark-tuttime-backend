package com.tuttime.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for Booking Sync Request
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingSyncRequest {
	private String bookingId;
	private String posSystem;  // e.g., iiko, R-Keeper
}
