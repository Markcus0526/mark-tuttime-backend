package com.tuttime.common.event;

import com.tuttime.common.enums.OrderStatus;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;


public class PosEvent {
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
