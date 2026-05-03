package com.tuttime.restaurant.repository.record;

import java.util.UUID;

public record TableHierarchyRecord(
		UUID tableId,
		UUID zoneId,
		UUID branchId,
		UUID restaurantId,
		String tableLabel,
		String zoneName,
		String branchName,
		String branchAddress,
		String branchPhone,
		String restaurantName,
		Long totalTables
) {
}

