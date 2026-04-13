package com.tuttime.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;
import java.util.UUID;

public class StaffDto {

	@Schema(description = "Staff request details")
	public record StaffRequest(
			@NotNull
			UUID userId,

			UUID branchId, // Optional: if null, they are restaurant-level staff

			@NotBlank
			String roleName, // "MANAGER", "HOSTESS", "WAITER"

			@NotNull
			boolean active
	) {
	}

	@Schema(description = "Staff request details")
	public record StaffResponse(
			UUID staffId,
			UUID restaurantId,
			UUID branchId,
			UUID userId,
			String fullName,
			String phoneNumber,
			String email,
			String roleName,
			boolean active,
			OffsetDateTime createdAt
	) {
	}

	@Schema(description = "Staff update request details")
	public record StaffUpdateRequest(
			String roleName,

			@NotNull
			Boolean active
	) {
	}
}
