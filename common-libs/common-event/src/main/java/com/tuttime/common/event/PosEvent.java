package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.OrderStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;


public class PosEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record PosSyncedEvent(
			String entityId,
			String entityType,  // BOOKING, ORDER, MENU
			String posSystem,
			String status,      // SUCCESS, FAILED
			String message,
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}
}
