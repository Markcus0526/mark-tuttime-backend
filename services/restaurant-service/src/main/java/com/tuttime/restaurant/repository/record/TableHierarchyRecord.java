package com.tuttime.restaurant.repository.record;

import java.util.UUID;

public record TableHierarchyRecord(
	UUID getRestaurantId,
	UUID getBranchId,
	UUID getZoneId,
	UUID getTableId
) {}

