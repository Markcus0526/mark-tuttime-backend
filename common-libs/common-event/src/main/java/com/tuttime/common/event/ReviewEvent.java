package com.tuttime.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public class ReviewEvent {

	/**
	 * Shared Record: Triggered when a moderator approves a review
	 */
	public record ReviewApprovedEvent(
			UUID reviewId,
			UUID restaurantId,
			UUID userId,
			int rating,
			String comment,
			String approvedAt
	)  implements BaseEvent {
	}
}
