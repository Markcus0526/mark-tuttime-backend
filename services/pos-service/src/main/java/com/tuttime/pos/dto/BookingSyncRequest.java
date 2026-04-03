package com.tuttime.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// DTO for Booking Sync Request
@Schema(description = "Request to trigger a manual Booking sync")
public record BookingSyncRequest(
		@NotNull @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
		UUID bookingId,
		@NotNull @Schema(example = "IIKO")
		String posSystem
) {
}
