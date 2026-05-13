package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.*;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.ModifierGroup;
import com.tuttime.menu.repository.MediaRepository;
import com.tuttime.menu.service.PricingService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mapper(
		componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE,
		uses = {ModifierMapper.class, MediaMapper.class})
public abstract class MenuItemMapper {

	@Autowired
	protected PricingService pricingService;

	@Autowired
	protected ModifierMapper modifierMapper;

	@Autowired
	protected MediaRepository mediaRepository;

	@Autowired
	protected MediaMapper mediaMapper;

	// --- 1. Detail Response (Single Item View) ---
	@Mapping(target = "menuItemId", source = "id")
	@Mapping(target = "categoryId", source = "category.id")
	@Mapping(target = "originalPrice", source = "price")
	@Mapping(target = "discountedPrice", ignore = true) // Calculated in afterMapping
	@Mapping(target = "modifierGroups", ignore = true)  // Merged in afterMapping
	@Mapping(target = "activeDiscountName", ignore = true)
	@Mapping(target = "medias", ignore = true)
	public abstract MenuItemDto.MenuItemDetailResponse toDetailResponse(MenuItem item);

	@Mapping(target = "menuItemId", source = "id")
	@Mapping(target = "currentPrice", ignore = true) // Calculated in afterMapping
	@Mapping(target = "imageUrl", ignore = true)        // Logic to fetch from Media table
	public abstract MenuItemDto.MenuItemResponse toSummaryResponse(MenuItem item);

	// --- 2. Full Menu Response (Bulk Map View) ---
	@Mapping(target = "itemId", source = "item.id")
	@Mapping(target = "name", source = "item.name")
	@Mapping(target = "description", source = "item.description")
	@Mapping(target = "originalPrice", source = "item.price")
	@Mapping(target = "discountedPrice", ignore = true)
	@Mapping(target = "imageUrl", ignore = true) // Handled in AfterMapping
	@Mapping(target = "tags", source = "itemTags") // Passed parameter
	public abstract FullMenuDto.FullMenuItemResponse toFullResponse(MenuItem item, List<MenuItemDto.TagResponse> itemTags);

	@AfterMapping
	protected void addImageUrl(MenuItem entity, @MappingTarget FullMenuDto.FullMenuItemResponse.FullMenuItemResponseBuilder builder) {
		// Logic to fetch primary image for the food item
		mediaRepository.findPrimaryImageUrlByEntityId(entity.getId())
				.ifPresent(builder::imageUrl);
	}

	/**
	 * Logic to merge Item + Category Modifiers and Calculate Prices
	 */
	@AfterMapping
	protected void postMappingLogic(MenuItem item, @MappingTarget MenuItemDto.MenuItemDetailResponse.MenuItemDetailResponseBuilder builder) {
		// 1. Calculate Prices
		DiscountDto.PricingResult result = pricingService.calculateFinalPrice(item);
		builder.discountedPrice(result.finalPrice());
		builder.originalPrice(item.getPrice());

		if (result.finalPrice() != null && result.finalPrice().compareTo(item.getPrice()) < 0) {
			builder.activeDiscountName(result.discountName());
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
	protected void postMappingSummaryLogic(MenuItem item, @MappingTarget MenuItemDto.MenuItemResponse.MenuItemResponseBuilder builder) {
		DiscountDto.PricingResult result = pricingService.calculateFinalPrice(item);
		builder.currentPrice(result.finalPrice());
	}

	@AfterMapping
	protected void addImageUrl(MenuItem item, @MappingTarget MenuItemDto.MenuItemResponse.MenuItemResponseBuilder builder) {
		// Find the primary image URL where entityId matches the categoryId
		mediaRepository.findFirstByEntityIdAndPrimaryEntityTrue(item.getId())
				.ifPresent(media -> builder.imageUrl(media.getUrl()));
	}

	@AfterMapping
	protected void addMedias(MenuItem item, @MappingTarget MenuItemDto.MenuItemDetailResponse.MenuItemDetailResponseBuilder builder) {
		// Fetch ALL Media (Images & Videos)
		List<MediaDto.MediaResponse> allMedia = mediaRepository.findAllByEntityIdAndDeletedFalse(item.getId())
				.stream()
				.map(mediaMapper::toResponse)
				.collect(Collectors.toList());

		builder.medias(allMedia);
	}

	@AfterMapping
	protected void addFullMenuImage(MenuItem entity, @MappingTarget FullMenuDto.FullMenuItemResponse.FullMenuItemResponseBuilder builder) {
		mediaRepository.findPrimaryImageUrlByEntityId(entity.getId())
				.ifPresent(builder::imageUrl);
	}

	@AfterMapping
	protected void postMappingDetailLogic(MenuItem item, @MappingTarget MenuItemDto.MenuItemDetailResponse.MenuItemDetailResponseBuilder builder) {
		DiscountDto.PricingResult result = pricingService.calculateFinalPrice(item);
		builder.discountedPrice(result.finalPrice());
		builder.originalPrice(item.getPrice());

		if (result.finalPrice() != null && result.finalPrice().compareTo(item.getPrice()) < 0) {
			builder.activeDiscountName(result.discountName());
		}

		// Merge Modifiers (Item + Category levels)
		Set<ModifierGroup> mergedGroups = new LinkedHashSet<>();
		if (item.getModifierGroups() != null) mergedGroups.addAll(item.getModifierGroups());
		if (item.getCategory() != null && item.getCategory().getModifierGroups() != null) {
			mergedGroups.addAll(item.getCategory().getModifierGroups());
		}

		builder.modifierGroups(mergedGroups.stream()
				.map(modifierMapper::toGroupResponse)
				.collect(Collectors.toList()));
	}

	@AfterMapping
	protected void addMediasToDetail(MenuItem item, @MappingTarget MenuItemDto.MenuItemDetailResponse.MenuItemDetailResponseBuilder builder) {
		List<MediaDto.MediaResponse> allMedia = mediaRepository.findAllByEntityIdAndDeletedFalse(item.getId())
				.stream()
				.map(mediaMapper::toResponse)
				.toList();
		builder.medias(allMedia);
	}
}
