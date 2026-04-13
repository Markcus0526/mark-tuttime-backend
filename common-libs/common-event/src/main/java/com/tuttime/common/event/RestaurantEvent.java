package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.OrderStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class RestaurantEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record StaffUpdatedEvent(
			UUID staffId,
			UUID userId,
			UUID restaurantId,
			UUID branchId,
			String roleName,
			Boolean active,
			OffsetDateTime timestamp
	) implements BaseEvent {
	}
}
