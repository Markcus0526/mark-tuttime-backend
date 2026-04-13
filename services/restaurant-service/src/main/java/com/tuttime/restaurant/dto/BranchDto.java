package com.tuttime.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BranchDto {

	@Schema(description = "Branch request details")
	public record BranchRequest(

		@NotNull(message = "Restaurant ID is required")
		UUID restaurantId,

		@NotBlank(message = "Address is required")
		@Size(max = 512, message = "Address must be at most 512 characters")
		String address,

		@Size(max = 20, message = "Phone must be at most 20 characters")
		@Pattern(regexp = "^\\+?[0-9\\-\\s]{7,20}$", message = "Phone must be a valid phone number")
		String phone,

		@NotBlank(message = "Timezone is required")
		@Size(max = 50, message = "Timezone must be at most 50 characters")
		String timezone,

		@DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be >= -180")
		@DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be <= 180")
		Double longitude,

		@DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be >= -90")
		@DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be <= 90")
		Double latitude
	) {
	}

	@Schema(description = "Branch response details")
	public record BranchResponse(
		UUID branchId,
		UUID restaurantId,
		String address,
		String phone,
		String timezone,
		Double longitude,
		Double latitude,
		OffsetDateTime createdAt,
		OffsetDateTime updatedAt
	) {
	}
}
