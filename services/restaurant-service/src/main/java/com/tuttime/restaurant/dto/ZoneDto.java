package com.tuttime.restaurant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ZoneDto {

	@Schema(description = "Zone request details")
	public record ZoneRequest(

			@NotNull(message = "Branch ID must not be null")
			UUID branchId,

			@NotBlank(message = "Name must not be blank")
			@Size(max = 255, message = "Name must be at most 255 characters")
			String name,

			@Size(max = 1000, message = "Description must be at most 1000 characters")
			String description,

			@NotNull(message = "Capacity must not be null")
			@Min(value = 1, message = "Capacity must be at least 1")
			Integer capacity,

			@NotNull(message = "closed must not be null")
			Boolean closed
	) {
	}

	@Schema(description = "Zone request details")
	public record ZoneResponse(

			UUID zoneId,
			UUID branchId,
			String name,
			String description,
			int capacity,
			boolean closed,
			OffsetDateTime createdAt,
			OffsetDateTime updatedAt
	) {
	}
}
