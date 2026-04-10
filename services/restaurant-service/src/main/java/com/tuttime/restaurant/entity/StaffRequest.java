package com.tuttime.restaurant.entity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

// Request DTO for assigning new staff
public record StaffRequest(
		@NotNull UUID userId,
		UUID branchId, // Optional: if null, they are restaurant-level staff

		@NotBlank
		String roleName, // "MANAGER", "HOSTESS", "WAITER"

		@NotBlank
		boolean isActive
) {}