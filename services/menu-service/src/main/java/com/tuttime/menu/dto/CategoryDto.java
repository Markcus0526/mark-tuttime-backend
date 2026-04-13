package com.tuttime.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public class CategoryDto {

	// Request payload for creating/updating a category
	@Schema(description = "Category request details")
	public record CategoryRequest(
			@NotNull(message = "Branch ID is required")
			UUID branchId,

			@NotBlank(message = "Category name cannot be empty")
			String name,

			String description,

			Integer priority
	) {
	}

	// Perfect for mapping a deep fetch with items (e.g., getting a full branch menu)
	@Schema(description = "Category response details")
	public record CategoryResponse(
			UUID id,

			UUID branchId,

			String name,

			String description,

			List<MenuItemDto.MenuItemResponseSummary> items
	) {
	}
}