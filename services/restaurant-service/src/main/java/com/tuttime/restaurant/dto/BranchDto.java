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

			@DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
			@DecimalMax(value = "90.0", message = "Latitude must be <= 90")
			Double latitude,

			@DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
			@DecimalMax(value = "180.0", message = "Longitude must be <= 180")
			Double longitude,

			@DecimalMin(value = "0.0", message = "Rating must be at least 0.0")
			BigDecimal minPrice,

			@DecimalMin(value = "0.0", message = "Rating must be at least 0.0")
			BigDecimal maxPrice,

			@Schema(description = "Status of the Branch", example = "false")
			boolean closed,

			Boolean favorite
	) {
	}

	@Schema(description = "Tag request details for branch association")
	public record TagRequest(
			@NotBlank(message = "Tag name is required")
			@Size(max = 50, message = "Tag name must be under 50 characters")
			String name,

			@NotBlank(message = "Tag icon is required")
			String iconUrl // Stores the Emoji (e.g., "🌶️")
	) {}

	@Schema(description = "Tag details for UI display")
	public record TagResponse(
			UUID id,
			String name,
			String iconUrl // Contains the Emoji (e.g., "🌿") or URL
	) {}

	@Schema(description = "Branch response")
	@Builder(toBuilder = true)
	public record BranchResponse(

			UUID branchId,

			String name,

			String description,

			String address,

			String phone,

			String timezone,

			Double latitude,

			Double longitude,

			BigDecimal rating,

			@NotNull(message = "closed must not be null")
			Boolean closed,

			String imageUrl,

			UUID restaurantId,

			String restaurantName,

			Double distance, // ST_Distance result

			BigDecimal minPrice,

			BigDecimal maxPrice,

			String priceRangeLabel, // e.g., "1200 ~ 2000"

			@Schema(description = "Count of currently available tables")
			Integer availableTables, // New field for "24 tables available"

			List<TagResponse> tags, // icons like 🌿, 🌶️

			Boolean favorite
	) {
	}

	@Schema(description = "Branch response details")
	@Builder(toBuilder = true)
	public record BranchDetailResponse(

			UUID branchId,

			String name,

			String description,

			String address,

			String phone,

			String timezone,

			Double latitude,

			Double longitude,

			BigDecimal rating,

			@Schema(description = "URL for all images and videos from Media table")
			List<MediaDto.MediaResponse> medias,

			UUID restaurantId,

			String restaurantName,

			BigDecimal minPrice,

			BigDecimal maxPrice,

			String priceRangeLabel, // e.g., "1200 ~ 2000"

			List<TagResponse> tags, // icons like 🌿, 🌶️

			Boolean favorite,

			List<ScheduleDto.ScheduleResponse> schedules,

			OffsetDateTime createdAt,

			OffsetDateTime updatedAt
	) {
	}
}
