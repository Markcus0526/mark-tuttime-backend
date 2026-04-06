package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


public class BookingEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record BookingConfirmedEvent(
			String restaurantId,
			String posSystem,
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}

	/**
	 * Shared Record: Must match the JSON structure sent by Booking Service
	 */
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record BookingCreatedEvent(
			UUID bookingId,
			UUID restaurantId,
			UUID userId,
			BigDecimal totalAmount,
			String status,
			String createdAt
	)  implements BaseEvent {
	}
}
