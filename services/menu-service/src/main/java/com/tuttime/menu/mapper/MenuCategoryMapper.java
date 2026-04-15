package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.MenuCategoryDto;
import com.tuttime.menu.dto.FullMenuDto;
import com.tuttime.menu.entity.MenuCategory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MenuItemMapper.class})
public interface MenuCategoryMapper {

	@Mapping(target = "categoryId", source = "id")
	FullMenuDto.CategoryWithItemsResponse toCategoryWithItemsResponse(MenuCategory category);

	@Mapping(target = "categoryId", source = "id")
	MenuCategoryDto.CategoryResponse toResponse(MenuCategory category);
}
