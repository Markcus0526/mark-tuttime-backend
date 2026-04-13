package com.tuttime.menu.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class FullMenuDto {

	// 1. Top Level: The Branch Menu
	public record FullMenuBranchMenuResponse(
			UUID branchId,
			List<FullMenuCategoryResponse> categories
	) {
	}

	// 2. Category Level
	public record FullMenuCategoryResponse(
			UUID menuCategoryId,
			String name,
			String description,
			List<FullMenuItemResponse> items
	) {
	}

	// 3. Item Level
	public record FullMenuItemResponse(
			UUID menuItemId,
			String name,
			String description,
			BigDecimal price,
			Boolean available,
			List<FullMenuModifierGroupResponse> modifierGroups
	) {
	}

	// 4. Modifier Group Level
	public record FullMenuModifierGroupResponse(
			UUID modifierGroupId,
			String name,
			Integer minSelect,
			Integer maxSelect,
			List<FullMenuModifierResponse> modifiers
	) {
	}

	// 5. Leaf Level: Individual Modifier
	public record FullMenuModifierResponse(
			UUID modifierId,
			String name,
			BigDecimal priceAdjustment
	) {
	}
}