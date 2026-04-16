package com.tuttime.menu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

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
			Boolean available,

			@Schema(description = "Display order priority", example = "1")
			Integer priority
	) {}


	@Schema(description = "Menu Item response with full customization and pricing logic")
	@Builder
	public record MenuItemDetailedResponse(
			UUID menuItemId,
			UUID categoryId,
			String name,
			String description,

			@Schema(description = "Base price set in database")
			BigDecimal originalPrice,

			@Schema(description = "Price after active discounts applied")
			BigDecimal discountedPrice,

			@Schema(description = "Name of the promotion applied", example = "Happy Hour")
			String activeDiscountName,

			Boolean available,

			@Schema(description = "Merged list: Item-specific + Inherited Category modifier groups")
			List<ModifierDto.ModifierGroupResponse> modifierGroups,

			@Schema(description = "URL for all images and videos from Media table")
			List<MediaDto.MediaResponse> medias
	) {}

	@Schema(description = "Compact summary response for category list views")
	@Builder
	public record MenuItemResponseSummary(
			UUID menuItemId,
			String name,
			@Schema(description = "The lower price (discounted) to show in lists")
			BigDecimal currentPrice,
			Boolean available,
			Integer priority,
			String imageUrl
	) {}
}