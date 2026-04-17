package com.tuttime.restaurant.repository.record;

import java.util.UUID;

public record TableDetailRecord(
		UUID tableId,
		UUID zoneId,
		String tableLabel,
		String zoneName,
		Integer seats
) {}
