package com.tuttime.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

public class StopListDto {

	@Schema(description = "Request payload to put an item on the stop list")
	public record StopListRequest(
			@NotNull(message = "Menu Item ID is required")
			UUID menuItemId,

			@NotNull(message = "Branch ID is required")
			UUID branchId,

			@NotNull(message = "Start date is required")
			@Schema(example = "2024-12-01")
			LocalDate startDate,

			@Schema(description = "Optional end date when item returns to stock", example = "2024-12-05")
			LocalDate endDate,

			@Schema(description = "Reason for stopping (e.g., Out of avocado)", example = "Supplier delay")
			String reason
	) {}

	@Schema(description = "Response payload for stop list entries")
	public record StopListResponse(
			UUID id,
			UUID menuItemId,
			String menuItemName, // Added for convenience in the frontend UI
			UUID branchId,
			LocalDate startDate,
			LocalDate endDate,
			String reason
	) {}
}