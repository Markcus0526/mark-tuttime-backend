package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.MenuItemDto;
import com.tuttime.menu.entity.MenuItemTag;
import com.tuttime.menu.entity.MenuTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MenuTagMapper {

	MenuItemDto.TagResponse toTagResponse(MenuTag tag);

	@Mapping(target = "id", source = "menuTag.id")
	@Mapping(target = "name", source = "menuTag.name")
	@Mapping(target = "iconUrl", source = "menuTag.iconUrl")
	MenuItemDto.TagResponse toTagResponse(MenuItemTag menuItemTag);
}