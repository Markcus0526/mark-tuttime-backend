package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;


public class PosEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record PosMenuUpdatedEvent(
			UUID branchId,
			List<UUID> itemIds,
			String updateType, // "DEACTIVATION" or "ACTIVATION"
			OffsetDateTime timestamp
	)  implements BaseEvent {
	}

	public record PosPriceUpdatedEvent(
			UUID branchId,
			UUID menuItemId,
			BigDecimal newPrice,
			OffsetDateTime timestamp
	) implements BaseEvent {
	}
}
