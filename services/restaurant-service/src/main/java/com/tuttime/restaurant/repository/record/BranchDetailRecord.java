package com.tuttime.restaurant.repository.record;

import java.math.BigDecimal;
import java.util.UUID;

public record BranchDetailRecord(
		UUID branchId,
		UUID restaurantId,
		String branchName,
		String branchAddress,
		String branchPhone,
		String restaurantName,
		BigDecimal minPrice,
		BigDecimal maxPrice
) {
}
