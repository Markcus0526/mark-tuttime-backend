package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.BranchRequest;
import com.tuttime.restaurant.dto.BranchResponse;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.util.DateMapper;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
		uses = DateMapper.class,
		unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BranchMapper {

	GeometryFactory geometryFactory = new GeometryFactory();

	Branch toEntity(BranchRequest request);

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