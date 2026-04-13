package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.FullMenuDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.entity.MenuItem;
import com.tuttime.menu.entity.Modifier;
import com.tuttime.menu.entity.ModifierGroup;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FullMenuMapper {

	/**
	 * Maps the entire database tree to a single Branch Menu Response.
	 */
	default FullMenuDto.FullMenuBranchMenuResponse toBranchMenuResponse(UUID branchId, List<MenuCategory> categories) {
		List<FullMenuDto.FullMenuCategoryResponse> categoryDtos = categories.stream()
				.map(this::toCategoryResponse)
				.collect(Collectors.toList());

		return new FullMenuDto.FullMenuBranchMenuResponse(branchId, categoryDtos);
	}

	default FullMenuDto.FullMenuCategoryResponse toCategoryResponse(MenuCategory category) {
		List<FullMenuDto.FullMenuItemResponse> itemDtos = category.getItems().stream()
				.map(this::toItemResponse)
				.collect(Collectors.toList());

		return new FullMenuDto.FullMenuCategoryResponse(
				category.getId(),
				category.getName(),
				category.getDescription(),
				itemDtos
		);
	}

	default FullMenuDto.FullMenuItemResponse toItemResponse(MenuItem item) {
		List<FullMenuDto.FullMenuModifierGroupResponse> groupDtos = item.getModifierGroups().stream()
				.map(this::toModifierGroupResponse)
				.collect(Collectors.toList());

		return new FullMenuDto.FullMenuItemResponse(
				item.getId(),
				item.getName(),
				item.getDescription(),
				item.getPrice(),
				item.getIsAvailable(),
				groupDtos
		);
	}

	default FullMenuDto.FullMenuModifierGroupResponse toModifierGroupResponse(ModifierGroup group) {
		List<FullMenuDto.FullMenuModifierResponse> modifierDtos = group.getModifiers().stream()
				.map(this::toModifierResponse)
				.collect(Collectors.toList());

		return new FullMenuDto.FullMenuModifierGroupResponse(
				group.getId(),
				group.getName(),
				group.getMinSelect(),
				group.getMaxSelect(),
				modifierDtos
		);
	}

	default FullMenuDto.FullMenuModifierResponse toModifierResponse(Modifier modifier) {
		return new FullMenuDto.FullMenuModifierResponse(
				modifier.getId(),
				modifier.getName(),
				modifier.getPriceAdjustment()
		);
	}
}
