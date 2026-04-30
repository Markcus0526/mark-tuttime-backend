package com.tuttime.common.event.kafka.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.OrderStatus;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

public class OrderEvent {

	@Builder(toBuilder = true)
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record OrderCreatedEvent(
			UUID eventId,
			UUID orderId,
			UUID branchId,
			OffsetDateTime occurredAt
	) implements BaseEvent {

		// Canonical Constructor to handle default values manually
		public OrderCreatedEvent {
			if (eventId == null) eventId = UUID.randomUUID();
			if (occurredAt == null) occurredAt = OffsetDateTime.now();
		}

		@Override
		public UUID eventId() {
			return eventId;
		}

		@Override
		public OffsetDateTime occurredAt() {
			return occurredAt;
		}
	}

	@Builder(toBuilder = true)
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record OrderStatusEvent(
			UUID eventId,
			UUID orderId,
			UUID bookingId,
			UUID branchId,
			OrderStatus oldStatus,
			OrderStatus newStatus,
			OffsetDateTime occurredAt
	) implements BaseEvent {

		// Canonical Constructor to handle default values manually
		public OrderStatusEvent {
			if (eventId == null) eventId = UUID.randomUUID();
			if (occurredAt == null) occurredAt = OffsetDateTime.now();
		}

		@Override
		public UUID eventId() {
			return eventId;
		}

		@Override
		public OffsetDateTime occurredAt() {
			return occurredAt;
		}
	}
}
