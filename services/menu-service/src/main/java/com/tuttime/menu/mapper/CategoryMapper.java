package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.CategoryDto;
import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.entity.MenuCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

	/**
	 * Converts a Request DTO from the frontend into a Database Entity.
	 */
	public MenuCategory toEntity(CategoryDto.CategoryRequest request) {
		if (request == null) return null;

		return MenuCategory.builder()
				.branchId(request.branchId())
				.name(request.name())
				.description(request.description())
				.build();
	}

	/**
	 * Converts a Database Entity into a Response DTO for the API.
	 * This version includes a summary of items if they are already loaded.
	 */
	public CategoryDto.CategoryResponse toResponse(MenuCategory entity) {
		if (entity == null) return null;

		List<MenuItemDto.MenuItemResponseSummary> itemSummaries = new ArrayList<>();

		// If items are loaded (e.g., via fetch join), map them to summaries
		if (entity.getItems() != null) {
			itemSummaries = entity.getItems().stream()
					.map(item -> new MenuItemDto.MenuItemResponseSummary(
							item.getId(),
							item.getName(),
							item.getPrice(),
							item.getIsAvailable(),
							item.getPriority()
					))
					.collect(Collectors.toList());
		}

		return new CategoryDto.CategoryResponse(
				entity.getId(),
				entity.getBranchId(),
				entity.getName(),
				entity.getDescription(),
				itemSummaries
		);
	}
}