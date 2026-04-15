package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.ModifierGroup;
import com.tuttime.menu.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE,
		uses = {ModifierMapper.class})
public abstract class MenuItemMapper {

	@Autowired
	protected PricingService pricingService;

	@Autowired
	protected ModifierMapper modifierMapper;

	@Mapping(target = "menuItemId", source = "id")
	@Mapping(target = "categoryId", source = "category.id")
	@Mapping(target = "originalPrice", source = "price")
	@Mapping(target = "discountedPrice", ignore = true) // Calculated in afterMapping
	@Mapping(target = "modifierGroups", ignore = true)  // Merged in afterMapping
	@Mapping(target = "imageUrl", ignore = true)        // Logic to fetch from Media table
	@Mapping(target = "activeDiscountName", ignore = true)
	public abstract MenuItemDto.MenuItemDetailedResponse toDetailedResponse(MenuItem item);

	@Mapping(target = "menuItemId", source = "id")
	@Mapping(target = "currentPrice", ignore = true) // Calculated in afterMapping
	public abstract MenuItemDto.MenuItemResponseSummary toSummaryResponse(MenuItem item);

	/**
	 * Logic to merge Item + Category Modifiers and Calculate Prices
	 */
	@AfterMapping
	protected void postMappingLogic(MenuItem item, @MappingTarget MenuItemDto.MenuItemDetailedResponse.MenuItemDetailedResponseBuilder builder) {
		// 1. Calculate Prices
		BigDecimal finalPrice = pricingService.calculateFinalPrice(item);
		builder.discountedPrice(finalPrice);
		builder.originalPrice(item.getPrice());

		if (finalPrice.compareTo(item.getPrice()) < 0) {
			builder.activeDiscountName("Promotion Applied");
		}

		// 2. Merge Modifiers
		Set<ModifierGroup> mergedGroups = new LinkedHashSet<>();
		if (item.getModifierGroups() != null) {
			mergedGroups.addAll(item.getModifierGroups());
		}

		if (item.getCategory() != null && item.getCategory().getModifierGroups() != null) {
			mergedGroups.addAll(item.getCategory().getModifierGroups());
		}

		// 3. Map to DTOs using the injected mapper
		List<ModifierDto.ModifierGroupResponse> modifierGroupResponses = mergedGroups.stream()
				.map(modifierMapper::toGroupResponse)
				.collect(Collectors.toList());

		builder.modifierGroups(modifierGroupResponses);
	}

	@AfterMapping
	protected void postMappingSummaryLogic(MenuItem item, @MappingTarget MenuItemDto.MenuItemResponseSummary.MenuItemResponseSummaryBuilder builder) {
		builder.currentPrice(pricingService.calculateFinalPrice(item));
	}
}
