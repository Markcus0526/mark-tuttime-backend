package com.tuttime.common.event;

import com.tuttime.common.enums.OrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderEvent {
	public record OrderCreatedEvent(
			String orderId,
			String posSystem,
			OffsetDateTime timestamp
	) implements BaseEvent {
	}

	public record OrderStatusEvent(
			UUID orderId,
			UUID bookingId,
			OrderStatus oldStatus,
			OrderStatus newStatus,
			OffsetDateTime timestamp
	) implements BaseEvent {
	}
}
