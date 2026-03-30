package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantMapper {

	RestaurantResponse toResponse(Restaurant entity);

	Restaurant toEntity(RestaurantRequest request);

	void updateEntityFromRequest(RestaurantRequest request, @MappingTarget Restaurant entity);
}
