package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.time.OffsetDateTime;
import java.util.UUID;


public class MenuEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record MenuUpdatedEvent(
			UUID branchId,
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}
}
