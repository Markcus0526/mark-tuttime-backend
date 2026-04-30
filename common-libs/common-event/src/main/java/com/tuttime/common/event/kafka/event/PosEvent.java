package com.tuttime.common.event.kafka.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


public class PosEvent {

	@Builder(toBuilder = true)
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record PosMenuUpdatedEvent(
			UUID eventId,
			UUID branchId,
			List<UUID> itemIds,
			String updateType, // "DEACTIVATION" or "ACTIVATION"
			OffsetDateTime occurredAt
	) implements BaseEvent {

		// Canonical Constructor to handle default values manually
		public PosMenuUpdatedEvent {
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
	public record PosPriceUpdatedEvent(
			UUID eventId,
			UUID branchId,
			UUID menuItemId,
			BigDecimal newPrice,
			OffsetDateTime occurredAt
	) implements BaseEvent {

		// Canonical Constructor to handle default values manually
		public PosPriceUpdatedEvent {
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
