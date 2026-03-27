package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.enums.MediaType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring") // This is the magic line
public interface MediaMapper {

	MediaDto mapToDto(Media media);

	Media mapToEntity(MediaDto dto);

	default Instant map(OffsetDateTime value) {
		return value == null ? null : value.toInstant();
	}

	default OffsetDateTime map(Instant value) {
		return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
	}
}