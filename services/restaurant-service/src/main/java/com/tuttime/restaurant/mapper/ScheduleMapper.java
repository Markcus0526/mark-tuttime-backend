package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.ScheduleRequest;
import com.tuttime.restaurant.dto.ScheduleResponse;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = DateMapper.class)
public interface ScheduleMapper {

	@Mapping(target = "parentId", ignore = true) // Handled by the Parent Entity relationship
	@Mapping(target = "isClosed", defaultValue = "false") // Or ignore if set by business logic
	ScheduleResponse toResponse(RestaurantSchedule entity);

	@Mapping(target = "scheduleId", ignore = true)
	@Mapping(target = "restaurantId", ignore = true) // Handled by the parent relationship in Service
	@Mapping(target = "deleted", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	RestaurantSchedule toEntity(ScheduleRequest request);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromRequest(ScheduleRequest request, @MappingTarget RestaurantSchedule entity);

}