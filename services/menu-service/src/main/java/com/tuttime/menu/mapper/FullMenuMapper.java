package com.tuttime.menu.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FullMenuMapper {
//
//	/**
//	 * Maps the entire database tree to a single Branch Menu Response.
//	 */
//	default FullMenuDto.FullMenuBranchMenuResponse toBranchMenuResponse(UUID branchId, List<MenuCategory> categories) {
//		List<FullMenuDto.FullMenuCategoryResponse> categoryDtos = categories.stream()
//				.map(this::toCategoryResponse)
//				.collect(Collectors.toList());
//
//		return new FullMenuDto.FullMenuBranchMenuResponse(branchId, categoryDtos);
//	}
//
//	default FullMenuDto.FullMenuCategoryResponse toCategoryResponse(MenuCategory category) {
//		List<FullMenuDto.FullMenuItemResponse> itemDtos = category.getItems().stream()
//				.map(this::toItemResponse)
//				.collect(Collectors.toList());
//
//		return new FullMenuDto.FullMenuCategoryResponse(
//				category.getId(),
//				category.getName(),
//				category.getDescription(),
//				category.getPriority(),
//				itemDtos
//		);
//	}
//
//	default FullMenuDto.FullMenuItemResponse toItemResponse(MenuItem item) {
//		List<FullMenuDto.FullMenuModifierGroupResponse> groupDtos = item.getModifierGroups().stream()
//				.map(this::toModifierGroupResponse)
//				.collect(Collectors.toList());
//
//		return new FullMenuDto.FullMenuItemResponse(
//				item.getId(),
//				item.getName(),
//				item.getDescription(),
//				item.getPrice(),
//				item.getAvailable(),
//				groupDtos
//		);
//	}
//
//	default FullMenuDto.FullMenuModifierGroupResponse toModifierGroupResponse(ModifierGroup group) {
//		List<FullMenuDto.FullMenuModifierResponse> modifierDtos = group.getModifiers().stream()
//				.map(this::toModifierResponse)
//				.collect(Collectors.toList());
//
//		return new FullMenuDto.FullMenuModifierGroupResponse(
//				group.getId(),
//				group.getName(),
//				group.getMinSelect(),
//				group.getMaxSelect(),
//				modifierDtos
//		);
//	}
//
//	default FullMenuDto.FullMenuModifierResponse toModifierResponse(Modifier modifier) {
//		return new FullMenuDto.FullMenuModifierResponse(
//				modifier.getId(),
//				modifier.getName(),
//				modifier.getPriceAdjustment()
//		);
//	}
}
