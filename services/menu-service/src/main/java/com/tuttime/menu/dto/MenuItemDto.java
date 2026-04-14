package com.tuttime.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class MenuItemDto {

	@Schema(description = "Menu Item request details")
	public record MenuItemRequest(

			@Schema(description = "The category this dish belongs to", example = "550e8400-e29b-41d4-a716-446655440000")
			@NotNull UUID categoryId,

			@Schema(description = "Dish name displayed to customer", example = "Bacon Double Cheeseburger")
			@NotBlank String name,

			@Schema(description = "Short ingredients description", example = "Two premium beef patties with crisp bacon")
			String description,

			@Schema(description = "Base price before modifications", example = "12.50")
			@NotNull @DecimalMin("0.00") BigDecimal price,

			@Schema(description = "If false, prevents customer selection", example = "true")
			Boolean available
	) {}

	// Detailed response with all groups and modifiers (for item selection in Flutter)
	@Schema(description = "Menu Item response details")
	public record MenuItemDetailedResponse(
			UUID menuItemId,
			UUID categoryId,
			String name,
			String description,
			BigDecimal price,
			Boolean available,
			List<ModifierDto.ModifierGroupResponse> modifierGroups
	) {}

	// Compact summary response (for basic list rendering)
	public record MenuItemResponseSummary(
			UUID menuItemId,
			String name,
			BigDecimal price,
			Boolean available,
			Integer priority
	) {}
}