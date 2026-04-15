package com.tuttime.menu.mapper;

import com.tuttime.menu.dto.MediaDto;
import com.tuttime.menu.entity.Media;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MediaMapper {

	@Mapping(target = "mediaId", source = "id")
	MediaDto.MediaResponse toResponse(Media media);

	@Mapping(target = "url", ignore = true)
	Media toEntity(MediaDto.MediaRequest request);
}