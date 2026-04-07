package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.BranchSchedule;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BranchScheduleMapper {

	ScheduleResponse toResponse(BranchSchedule entity);

	BranchSchedule toEntity(ScheduleRequest request);

	void updateEntityFromRequest(ScheduleRequest request, @MappingTarget BranchSchedule entity);

}