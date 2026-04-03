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
			UUID id,
			String name,
			String description,
			List<FullMenuItemResponse> items
	) {
	}

	// 3. Item Level
	public record FullMenuItemResponse(
			UUID id,
			String name,
			String description,
			BigDecimal price,
			Boolean isAvailable,
			List<FullMenuModifierGroupResponse> modifierGroups
	) {
	}

	// 4. Modifier Group Level
	public record FullMenuModifierGroupResponse(
			UUID id,
			String name,
			Integer minSelect,
			Integer maxSelect,
			List<FullMenuModifierResponse> modifiers
	) {
	}

	// 5. Leaf Level: Individual Modifier
	public record FullMenuModifierResponse(
			UUID id,
			String name,
			BigDecimal priceAdjustment
	) {
	}
}