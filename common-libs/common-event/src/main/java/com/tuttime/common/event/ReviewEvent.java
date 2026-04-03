package com.tuttime.common.event;

import java.math.BigDecimal;
import java.util.UUID;

public class ReviewEvent {

	/**
	 * Shared event record for Kafka topics
	 */
	public record RatingUpdateEvent(
			UUID restaurantId,
			BigDecimal newAverage,
			int totalReviews
	)  implements BaseEvent {
	}
}
