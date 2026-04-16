package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.MenuCategoryDto;
import com.tuttime.menu.dto.FullMenuDto;
import com.tuttime.menu.entity.MenuCategory;
import com.tuttime.menu.repository.MediaRepository;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {MenuItemMapper.class})
public abstract class MenuCategoryMapper {

	@Autowired
	protected MediaRepository mediaRepository;

	@Mapping(target = "categoryId", source = "id")
	public abstract FullMenuDto.CategoryWithItemsResponse toCategoryWithItemsResponse(MenuCategory category);

	@Mapping(target = "categoryId", source = "id")
	@Mapping(target = "imageUrl", ignore = true)
	public abstract MenuCategoryDto.CategoryResponse toResponse(MenuCategory category);

	@AfterMapping
	protected void addImageUrl(MenuCategory category, @MappingTarget MenuCategoryDto.CategoryResponse.CategoryResponseBuilder builder) {
		// Find the primary image URL where entityId matches the categoryId
		mediaRepository.findPrimaryImageUrlByEntityId(category.getId())
				.ifPresent(builder::imageUrl);
	}
}
