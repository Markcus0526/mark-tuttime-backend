package com.tuttime.restaurant.repository.record;

import java.util.UUID;

public record TableBookingInfo(
		UUID tableId,
		UUID zoneId,
		String tableLabel,
		String zoneName,
		Integer seats
) {}
