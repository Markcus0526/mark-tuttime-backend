package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.dto.ZoneRequest;
import com.tuttime.restaurant.dto.ZoneResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.Zone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring") // This is the magic line
public interface ZoneMapper {

	@Mapping(target = "zoneId", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	@Mapping(target = "isClosed", ignore = true) // From your previous error
	Zone toEntity(ZoneDto dto);

	@Mapping(target = "zoneId", ignore = true)
	@Mapping(target = "isClosed", ignore = true) // <--- Add this line
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	ZoneResponse toResponse(Zone zone);

	Zone fromRequest(ZoneRequest request);

	default OffsetDateTime map(Instant value) {
		return value == null ? null : value.atOffset(ZoneOffset.UTC);
	}
}

