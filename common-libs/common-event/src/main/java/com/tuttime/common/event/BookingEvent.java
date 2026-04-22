package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


public class BookingEvent {

	/**
	 * Shared Record: Must match the JSON structure sent by Booking Service
	 */
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record BookingStatusUpdatedEvent(
			UUID bookingId,
			UUID tableId,
			UUID branchId,
			UUID userId,
			BookingStatus oldStatus,
			BookingStatus newStatus,
			OffsetDateTime bookingTime,
			UUID changedBy,
			String reason,
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}
}
