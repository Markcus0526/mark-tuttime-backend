package com.tuttime.common.event;

import java.time.OffsetDateTime;


public class MenuEvent {
	public record MenuUpdatedEvent(
			String restaurantId,
			String posSystem,
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}
}
