package com.tuttime.restaurant.repository.record;

import java.util.UUID;

public record BranchDetailRecord(
	UUID branchId,
	UUID restaurantId,
	String branchName,
	String branchAddress,
	String branchPhone,
	String restaurantName,
	String restaurantAddress,
	String restaurantPhone
) {}
