package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MediaMapper {

	MediaDto.MediaResponse toResponse(Media entity);

	Media toEntity(MediaDto.MediaRequest request);

	void updateEntityFromRequest(MediaDto.MediaRequest request, @MappingTarget Media entity);
}