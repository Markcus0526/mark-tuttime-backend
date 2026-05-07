package com.tuttime.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

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
			List<FullMenuItemResponse> items
	) {
	}

	@Schema(description = "Master response for the entire branch menu")
	public record FullMenuBranchMenuResponse(
			UUID branchId,
			List<CategoryWithItemsResponse> categories
	) {
	}

	/// /////////////////////////////////////////////////////////
	///
	// 3. Item Level
	@Builder(toBuilder = true)
	public record FullMenuItemResponse(
			UUID itemId,
			String name,
			String description,
			BigDecimal originalPrice,
			BigDecimal discountedPrice, // Calculated if a discount applies
			String imageUrl,
			List<MenuItemDto.TagResponse> tags, // The 🌱 🔥 tags from Figma
			List<FullMenuModifierGroupResponse> modifierGroups
	) {
		public boolean hasDiscount() {
			return discountedPrice != null && discountedPrice.compareTo(originalPrice) < 0;
		}
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