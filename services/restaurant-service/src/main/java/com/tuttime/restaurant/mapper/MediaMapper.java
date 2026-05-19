package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Media;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MediaMapper {

    @Mapping(target = "mediaId", source = "id")
    MediaDto.MediaResponse toResponse(Media entity);

    Media toEntity(MediaDto.MediaRequest request);

    void updateEntityFromRequest(MediaDto.MediaRequest request, @MappingTarget Media entity);
}