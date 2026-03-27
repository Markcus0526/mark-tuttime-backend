package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.BranchSchedule;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ScheduleMapper {

	// RestaurantSchedule mappings

	@Mapping(target = "scheduleId", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	RestaurantSchedule toRestaurantSchedule(ScheduleRequest request);

	ScheduleResponse toScheduleResponse(RestaurantSchedule entity);

	// BranchSchedule mappings

	@Mapping(target = "scheduleId", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	BranchSchedule toBranchSchedule(ScheduleRequest request);

	ScheduleResponse toScheduleResponse(BranchSchedule entity);

	// Update existing RestaurantSchedule from request
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateRestaurantScheduleFromRequest(ScheduleRequest request, @MappingTarget RestaurantSchedule entity);

	// Update existing BranchSchedule from request
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateBranchScheduleFromRequest(ScheduleRequest request, @MappingTarget BranchSchedule entity);
}