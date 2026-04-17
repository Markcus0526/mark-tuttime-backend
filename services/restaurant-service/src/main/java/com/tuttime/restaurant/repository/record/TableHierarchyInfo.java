package com.tuttime.restaurant.repository.record;

import java.util.UUID;

public record TableHierarchyInfo(
	UUID getRestaurantId,
	UUID getBranchId,
	UUID getZoneId,
	UUID getTableId
) {}

