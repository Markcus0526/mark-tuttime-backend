package com.tuttime.common.event;

import java.time.OffsetDateTime;


public class BookingEvent {
	public record BookingConfirmedEvent(
			String restaurantId,
			String posSystem,
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}
}
