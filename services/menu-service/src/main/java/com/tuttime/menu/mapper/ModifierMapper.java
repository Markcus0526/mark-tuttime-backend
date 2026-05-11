package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.ModifierDto;
import com.tuttime.menu.entity.Modifier;
import com.tuttime.menu.entity.ModifierGroup;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ModifierMapper {

	// Converts Group Entity -> Group Response DTO
	@Mapping(target = "modifierGroupId", source = "id")
	ModifierDto.ModifierGroupResponse toGroupResponse(ModifierGroup group);

	// Converts Individual Modifier Entity -> Modifier Response DTO
	@Mapping(target = "modifierId", source = "id")
	ModifierDto.ModifierResponse toModifierResponse(Modifier modifier);

	@Mapping(target = "branchId", source = "branchId")
	ModifierGroup toEntity(ModifierDto.ModifierGroupRequest request);
}
