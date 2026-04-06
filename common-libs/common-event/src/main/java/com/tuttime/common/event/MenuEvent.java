package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;


public class MenuEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record MenuUpdatedEvent(
			String restaurantId,
			String posSystem,
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}
}
