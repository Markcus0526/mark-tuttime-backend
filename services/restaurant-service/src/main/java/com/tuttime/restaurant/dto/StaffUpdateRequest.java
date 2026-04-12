package com.tuttime.restaurant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Request DTO for updates
public record StaffUpdateRequest(
		String roleName,

		@NotNull
		Boolean active
) {}
