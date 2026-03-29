package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Restaurant;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RestaurantMapper {

	RestaurantResponse toResponse(Restaurant entity);

	@Mapping(target = "restaurantId", ignore = true)
	@Mapping(target = "location", ignore = true)
	@Mapping(target = "deleted", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	Restaurant toEntity(RestaurantRequest request);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromRequest(RestaurantRequest request, @MappingTarget Restaurant entity);
}
