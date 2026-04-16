package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.repository.MediaRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE,
		uses = {MediaMapper.class})
public abstract class RestaurantMapper {

	@Autowired
	protected MediaRepository mediaRepository;

	@Autowired
	protected MediaMapper mediaMapper;

	@Mapping(target = "restaurantId", source = "id")
	@Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
	@Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
	@Mapping(target = "imageUrl", ignore = true)
	public abstract RestaurantDto.RestaurantResponse toResponse(Restaurant entity);

	@Mapping(target = "restaurantId", source = "id")
	@Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
	@Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
	@Mapping(target = "medias", ignore = true)
	public abstract RestaurantDto.RestaurantDetailedResponse toDetailedResponse(Restaurant entity);

	@Mapping(target = "location", expression = "java(toPoint(request.longitude(), request.latitude()))")
	public abstract Restaurant toEntity(RestaurantDto.RestaurantRequest request);

	@Mapping(target = "location", expression = "java(toPoint(request.longitude(), request.latitude()))")
	public abstract void updateEntityFromRequest(RestaurantDto.RestaurantRequest request, @MappingTarget Restaurant entity);

	@AfterMapping
	protected void addImageUrl(Restaurant entity, @MappingTarget RestaurantDto.RestaurantResponse.RestaurantResponseBuilder builder) {
		// Uses the logic from Page 9 of your PDF (entity_id + is_primary)
		mediaRepository.findPrimaryImageUrlByEntityId(entity.getId())
				.ifPresent(builder::imageUrl);
	}

	@AfterMapping
	protected void addMedias(Restaurant item, @MappingTarget RestaurantDto.RestaurantDetailedResponse.RestaurantDetailedResponseBuilder builder) {
		// Fetch ALL Media (Images & Videos)
		List<MediaDto.MediaResponse> allMedia = mediaRepository.findAllImageAndVideoByEntityId(item.getId())
				.stream()
				.map(mediaMapper::toResponse)
				.collect(Collectors.toList());

		builder.medias(allMedia);
	}

	// Reusable helper for JTS Points
	protected Point toPoint(Double lon, Double lat) {
		if (lon == null || lat == null) return null;
		return new GeometryFactory().createPoint(new Coordinate(lon, lat));
	}
}
