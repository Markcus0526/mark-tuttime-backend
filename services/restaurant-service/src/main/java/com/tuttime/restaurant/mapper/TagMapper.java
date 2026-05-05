package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.entity.BranchTag;
import com.tuttime.restaurant.entity.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface TagMapper {

	@Mapping(target = "id", source = "id")
	@Mapping(target = "name", source = "name")
	@Mapping(target = "iconUrl", source = "iconUrl")
	BranchDto.TagResponse toTagResponse(Tag tag);

	@Mapping(target = "id", source = "tag.id")
	@Mapping(target = "name", source = "tag.name")
	@Mapping(target = "iconUrl", source = "tag.iconUrl")
	BranchDto.TagResponse toTagResponse(BranchTag branchTag);
}