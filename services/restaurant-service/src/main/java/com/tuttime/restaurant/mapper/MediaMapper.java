package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MediaMapper {

	MediaResponse toResponse(Media entity);

	Media toEntity(MediaRequest request);

	void updateEntityFromRequest(MediaRequest request, @MappingTarget Media entity);
}