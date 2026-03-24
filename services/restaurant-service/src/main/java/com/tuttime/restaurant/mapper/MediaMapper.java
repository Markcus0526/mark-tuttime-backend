package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.MediaType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // This is the magic line
public class MediaMapper {

    public MediaDto toDto(Media entity) {
        if (entity == null) return null;
        MediaDto dto = new MediaDto();
        dto.setMediaId(entity.getMediaId());
        dto.setEntityType(entity.getEntityType());
        dto.setEntityId(entity.getEntityId());
        dto.setUrl(entity.getUrl());
        dto.setAltText(entity.getAltText());
        dto.setIsPrimary(entity.getIsPrimary());
        dto.setMediaType(entity.getMediaType());
        return dto;
    }

    public Media toEntity(MediaDto dto) {
        if (dto == null) return null;
        Media entity = new Media();
        entity.setMediaId(dto.getMediaId());
        entity.setEntityType(dto.getEntityType());
        entity.setEntityId(dto.getEntityId());
        entity.setUrl(dto.getUrl());
        entity.setAltText(dto.getAltText());
        entity.setIsPrimary(dto.getIsPrimary() != null ? dto.getIsPrimary() : false);
        entity.setMediaType(dto.getMediaType() != null ? dto.getMediaType() : MediaType.IMAGE);
        return entity;
    }
}

