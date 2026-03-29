package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.BranchRequest;
import com.tuttime.restaurant.dto.BranchResponse;
import com.tuttime.restaurant.entity.Branch;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BranchMapper {

	GeometryFactory geometryFactory = new GeometryFactory();

	@Mapping(target = "location", expression = "java(toPoint(request.getLongitude(), request.getLatitude()))")
	@Mapping(target = "branchId", ignore = true)
	@Mapping(target = "isDeleted", ignore = true)
	@Mapping(target = "deletedAt", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	Branch toEntity(BranchRequest request);

	@Mapping(target = "longitude", expression = "java(fromPoint(entity.getLocation()) != null ? fromPoint(entity.getLocation()).x : null)")
	@Mapping(target = "latitude", expression = "java(fromPoint(entity.getLocation()) != null ? fromPoint(entity.getLocation()).y : null)")
	BranchResponse toResponse(Branch entity);

	default Point toPoint(Double longitude, Double latitude) {
		if (longitude == null || latitude == null) return null;
		return geometryFactory.createPoint(new Coordinate(longitude, latitude));
	}

	default Coordinate fromPoint(Point point) {
		if (point == null) return null;
		return point.getCoordinate();
	}
}