package com.tuttime.restaurant.entity;

import java.time.OffsetDateTime;
import java.util.UUID;

// Response DTO
public record StaffResponse(
		UUID staffId,
		UUID restaurantId,
		UUID branchId,
		UUID userId,
		String roleName,
		boolean isActive,
		OffsetDateTime createdAt
) {}