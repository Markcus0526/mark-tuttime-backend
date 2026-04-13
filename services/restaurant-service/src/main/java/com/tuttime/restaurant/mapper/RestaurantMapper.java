package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantMapper {

	@Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
	@Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
	RestaurantDto.RestaurantResponse toResponse(Restaurant entity);

	Restaurant toEntity(RestaurantDto.RestaurantRequest request);

	@Mapping(target = "location", ignore = true)
	void updateEntityFromRequest(RestaurantDto.RestaurantRequest request, @MappingTarget Restaurant entity);
}
