package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ZoneMapper {

	Zone toEntity(ZoneDto.ZoneRequest request);

	ZoneDto.ZoneResponse toResponse(Zone zone);

	void updateEntityFromRequest(ZoneDto.ZoneRequest request, @MappingTarget Zone entity);

	default OffsetDateTime map(Instant value) {
		return value == null ? null : value.atOffset(ZoneOffset.UTC);
	}
}

