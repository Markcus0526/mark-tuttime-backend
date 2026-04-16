package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.dto.RestaurantDto;
import com.tuttime.restaurant.entity.Branch;
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
public abstract class BranchMapper {

	@Autowired
	protected MediaRepository mediaRepository;

	@Autowired
	protected MediaMapper mediaMapper;

	@Mapping(target = "branchId", source = "id")
	@Mapping(target = "restaurantId", source = "restaurant.id")
	@Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
	@Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
	@Mapping(target = "imageUrl", ignore = true)
	public abstract BranchDto.BranchResponse toResponse(Branch entity);

	@Mapping(target = "branchId", source = "id")
	@Mapping(target = "restaurantId", source = "restaurant.id")
	@Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
	@Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
	@Mapping(target = "medias", ignore = true)
	public abstract BranchDto.BranchDetailedResponse toDetailedResponse(Branch entity);

	@Mapping(target = "location", expression = "java(toPoint(request.longitude(), request.latitude()))")
	public abstract Branch toEntity(BranchDto.BranchRequest request);

	@Mapping(target = "location", expression = "java(toPoint(request.longitude(), request.latitude()))")
	public abstract void updateEntityFromRequest(BranchDto.BranchRequest request, @MappingTarget Branch entity);

	@AfterMapping
	protected void addImageUrl(Branch entity, @MappingTarget BranchDto.BranchResponse.BranchResponseBuilder builder) {
		// Uses the logic from Page 9 of your PDF (entity_id + is_primary)
		mediaRepository.findPrimaryImageUrlByEntityId(entity.getId())
				.ifPresent(builder::imageUrl);
	}

	@AfterMapping
	protected void addMedias(Branch item, @MappingTarget BranchDto.BranchDetailedResponse.BranchDetailedResponseBuilder builder) {
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