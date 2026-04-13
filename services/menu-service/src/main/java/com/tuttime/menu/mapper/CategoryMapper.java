package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.CategoryDto;
import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.entity.MenuCategory;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CategoryMapper {

	/**
	 * Converts a Request DTO from the frontend into a Database Entity.
	 */
	MenuCategory toEntity(CategoryDto.CategoryRequest request);

	/**
	 * Use the 'default' keyword to allow a method body in an interface.
	 */
	default CategoryDto.CategoryResponse toResponse(MenuCategory entity) {
		if (entity == null) return null;

		List<MenuItemDto.MenuItemResponseSummary> itemSummaries = new ArrayList<>();

		// Ensure getItems() exists in MenuCategory entity
		if (entity.getItems() != null) {
			itemSummaries = entity.getItems().stream()
					.map(item -> new MenuItemDto.MenuItemResponseSummary(
							item.getMenuItemId(),
							item.getName(),
							item.getPrice(),
							item.getAvailable(),
							item.getPriority()
					))
					.collect(Collectors.toList());
		}

		return new CategoryDto.CategoryResponse(
				entity.getMenuCategoryId(),
				entity.getBranchId(),
				entity.getName(),
				entity.getDescription(),
				itemSummaries
		);
	}
}