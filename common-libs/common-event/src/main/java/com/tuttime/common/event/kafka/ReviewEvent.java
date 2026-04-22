package com.tuttime.common.event.kafka;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewEvent {

	/**
	 * Shared Record: Triggered when a moderator approves a review
	 */
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record ReviewApprovedEvent(
			UUID eventId,
			UUID reviewId,
			UUID branchId,
			UUID userId,
			Integer rating,
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}
}
