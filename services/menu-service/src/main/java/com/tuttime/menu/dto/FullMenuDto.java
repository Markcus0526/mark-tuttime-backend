package com.tuttime.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class FullMenuDto {

	@Schema(description = "Category containing its nested items")
	public record CategoryWithItemsResponse(
			UUID categoryId,
			String name,
			String description,
			Integer priority,
			List<MenuItemDto.MenuItemResponseSummary> items
	) {}

	@Schema(description = "Master response for the entire branch menu")
	public record FullMenuBranchMenuResponse(
			UUID branchId,
			List<CategoryWithItemsResponse> categories
	) {}

	////////////////////////////////////////////////////////////
	// 2. Category Level
	public record FullMenuCategoryResponse(
			UUID categoryId,
			String name,
			String description,
			Integer priority,
			List<FullMenuItemResponse> items
	) {}

	// 3. Item Level
	public record FullMenuItemResponse(
			UUID menuItemId,
			String name,
			String description,
			BigDecimal price,
			Boolean available,
			List<FullMenuModifierGroupResponse> modifierGroups
	) {}

	// 4. Modifier Group Level
	public record FullMenuModifierGroupResponse(
			UUID modifierGroupId,
			String name,
			Integer minSelect,
			Integer maxSelect,
			List<FullMenuModifierResponse> modifiers
	) {}

	// 5. Leaf Level: Individual Modifier
	public record FullMenuModifierResponse(
			UUID modifierId,
			String name,
			BigDecimal priceAdjustment
	) {}
}