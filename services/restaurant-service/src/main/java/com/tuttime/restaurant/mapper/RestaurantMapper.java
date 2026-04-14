package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.entity.Restaurant;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface RestaurantMapper {

	@Mapping(source = "id", target = "restaurantId")
	@Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
	@Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
	RestaurantDto.RestaurantResponse toResponse(Restaurant entity);

	@Mapping(target = "location", expression = "java(toPoint(request.longitude(), request.latitude()))")
	Restaurant toEntity(RestaurantDto.RestaurantRequest request);

	@Mapping(target = "location", expression = "java(toPoint(request.longitude(), request.latitude()))")
	void updateEntityFromRequest(RestaurantDto.RestaurantRequest request, @MappingTarget Restaurant entity);

	// Reusable helper for JTS Points
	default Point toPoint(Double lon, Double lat) {
		if (lon == null || lat == null) return null;
		return new GeometryFactory().createPoint(new Coordinate(lon, lat));
	}
}
