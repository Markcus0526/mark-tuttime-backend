package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.OrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record OrderCreatedEvent(
			UUID orderId,
			String posSystem,
			OffsetDateTime timestamp
	) implements BaseEvent {
	}

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record OrderStatusEvent(
			UUID orderId,
			UUID bookingId,
			OrderStatus oldStatus,
			OrderStatus newStatus,
			OffsetDateTime timestamp
	) implements BaseEvent {
	}
}
