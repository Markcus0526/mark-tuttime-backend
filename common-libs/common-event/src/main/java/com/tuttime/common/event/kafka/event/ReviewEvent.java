package com.tuttime.common.event.kafka.event;

import com.tuttime.common.enums.ReviewStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewEvent {

	/**
	 * Shared Record: Triggered when a moderator approves a review
	 */
	@Builder(toBuilder = true)
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record ReviewUpdatedEvent(
			UUID eventId,
			UUID reviewId,
			UUID branchId,
			UUID userId,
			Integer rating,
			ReviewStatus status,
			OffsetDateTime occurredAt
	) implements BaseEvent {

		// Canonical Constructor to handle default values manually
		public ReviewUpdatedEvent {
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
