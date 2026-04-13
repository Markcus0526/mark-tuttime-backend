package com.tuttime.restaurant.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

// Response DTO
public record StaffResponse(
		UUID staffId,
		UUID restaurantId,
		UUID branchId,
		UUID userId,
		String roleName,
		boolean active,
		OffsetDateTime createdAt
) {
}