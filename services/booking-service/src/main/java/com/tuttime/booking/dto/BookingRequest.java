package com.tuttime.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable DTO for creating a new booking.
 */
public record BookingRequest(
		@NotNull(message = "User ID is required")
		UUID userId,

		@NotNull(message = "Restaurant ID is required")
		UUID restaurantId,

		@NotNull(message = "Branch ID is required")
		UUID branchId,

		@NotNull(message = "Zone ID is required")
		UUID zoneId,

		@NotNull(message = "Table ID is required")
		UUID tableId,

		@NotNull(message = "Booking time is required")
		@Future(message = "Booking must be in the future")
		Instant bookingDatetime,

		@NotNull(message = "Duration is required")
		@Min(value = 15, message = "Minimum duration is 15 minutes")
		Integer duration,

		@NotNull(message = "Party size is required")
		@Min(value = 1, message = "Party size must be at least 1")
		Integer partySize,

		Integer leadTimeMinutes,

		@Min(value = 0)
		Integer gracePeriodMins
) {
	// Compact constructor for default values
	public BookingRequest {
		if (leadTimeMinutes == null) leadTimeMinutes = 0;
		if (gracePeriodMins == null) gracePeriodMins = 15;
	}
}
