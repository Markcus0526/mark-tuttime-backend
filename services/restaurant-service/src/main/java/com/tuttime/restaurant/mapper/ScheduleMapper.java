package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ScheduleDto;
import com.tuttime.restaurant.entity.Schedule;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ScheduleMapper {

	ScheduleDto.ScheduleResponse toResponse(Schedule entity);

	Schedule toEntity(ScheduleDto.ScheduleRequest request);

	void updateEntityFromRequest(ScheduleDto.ScheduleRequest request, @MappingTarget Schedule entity);

}