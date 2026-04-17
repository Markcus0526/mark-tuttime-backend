package com.tuttime.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class BranchDto {

	@Schema(description = "Branch request details")
	public record BranchRequest(

			@NotNull(message = "Restaurant ID is required")
			UUID restaurantId,

			@NotBlank(message = "Restaurant name is required")
			@Size(max = 255, message = "Restaurant name must be at most 255 characters")
			String name,

			@Size(max = 5000, message = "Description can be up to 5000 characters")
			String description,

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
			Double latitude,

			@DecimalMin(value = "0.0", inclusive = true, message = "Rating must be at least 0.0")
			@DecimalMax(value = "5.0", inclusive = true, message = "Rating must be at most 5.0")
			BigDecimal rating
	) {
	}

	@Schema(description = "Branch response")
	@Builder
	public record BranchResponse(

			UUID branchId,
			UUID restaurantId,
			String name,
			String description,
			String address,
			String phone,
			String timezone,
			Double longitude,
			Double latitude,
			BigDecimal rating,
			String imageUrl,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt
	) {
	}

	@Schema(description = "Branch response details")
	@Builder
	public record BranchDetailResponse(

			UUID branchId,

			UUID restaurantId,

			String name,

			String description,

			String address,

			String phone,

			String timezone,

			Double longitude,

			Double latitude,

			BigDecimal rating,

			@Schema(description = "URL for all images and videos from Media table")
			List<MediaDto.MediaResponse> medias,

			OffsetDateTime createdAt,

			OffsetDateTime updatedAt
	) {
	}
}
