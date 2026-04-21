package com.tuttime.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class PosSyncDto {

	@Schema(description = "Request to trigger a manual Booking sync")
	public record BookingSyncRequest(
			@NotNull(message = "Booking ID is required")
			@NotNull @Schema(example = "550e8400-e29b-41d4-a716-446655440000")
			UUID bookingId,

			@NotNull(message = "Branch ID is required")
			@NotNull @Schema(example = "88e7b8a0-2f3b-4c5d-ae6f-7890abcd1234")
			UUID branchId,

			@Schema(
					type = "string",
					format = "date-time",
					example = "2026-04-21T14:30:00Z", // Provide a concrete example
					description = "ISO-8601 formatted booking time"
			)
			OffsetDateTime bookingTime
	) {
	}

	@Schema(description = "Request to sync Menu catalog from POS")
	public record MenuSyncRequest(

			@NotNull @Schema(example = "88e7b8a0-2f3b-4c5d-ae6f-7890abcd1234")
			UUID branchId
	) {
	}

	@Schema(description = "Request to trigger a manual Order sync")
	public record OrderSyncRequest(

			@NotNull @Schema(example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
			UUID orderId,

			@NotNull @Schema(example = "a1b2c3d4-e5f6-7g8h-9i0j-k1l2m3n4o5p6")
			UUID branchId
	) {
	}
}
