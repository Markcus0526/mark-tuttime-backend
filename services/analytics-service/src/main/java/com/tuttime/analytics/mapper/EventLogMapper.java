package com.tuttime.analytics.mapper;

import com.tuttime.analytics.dto.EventLogDto;
import com.tuttime.analytics.entity.EventLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface EventLogMapper {
	EventLogMapper INSTANCE = Mappers.getMapper(EventLogMapper.class);

	EventLogDto toDTO(EventLog entity);
	EventLog toEntity(EventLogDto dto);
}
