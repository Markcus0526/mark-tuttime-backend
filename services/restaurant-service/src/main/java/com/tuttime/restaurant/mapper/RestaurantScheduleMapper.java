package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantScheduleMapper {

	ScheduleResponse toResponse(RestaurantSchedule entity);

	RestaurantSchedule toEntity(ScheduleRequest request);

	void updateEntityFromRequest(ScheduleRequest request, @MappingTarget RestaurantSchedule entity);

}