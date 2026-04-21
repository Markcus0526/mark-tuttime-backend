package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.StopListDto;
import com.tuttime.menu.entity.StopList;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface StopListMapper {

	// Converts Group Entity -> Group Response DTO
	@Mapping(target = "stopListId", source = "id")
	@Mapping(target = "menuItemId", source = "menuItem.id")
	@Mapping(target = "menuItemName", source = "menuItem.name")
	StopListDto.StopListResponse toResponse(StopList stopList);
}
