package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.entity.BranchTag;
import com.tuttime.restaurant.entity.BranchTagId;
import com.tuttime.restaurant.entity.Tag;
import com.tuttime.restaurant.repository.MediaRepository;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
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
    @Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
    @Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "restaurantName", source = "restaurant.name")
    @Mapping(target = "tags", source = "branchTags")
    public abstract BranchDto.BranchResponse toResponse(Branch entity);

    @Mapping(target = "branchId", source = "id")
    @Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
    @Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
    @Mapping(target = "medias", ignore = true)
    @Mapping(target = "schedules", ignore = true)
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "restaurantName", source = "restaurant.name")
    @Mapping(target = "tags", source = "branchTags")
    public abstract BranchDto.BranchDetailResponse toDetailResponse(Branch entity);

    @Mapping(target = "branchId", source = "id")
    @Mapping(target = "longitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getX() : null)")
    @Mapping(target = "latitude", expression = "java(entity.getLocation() != null ? entity.getLocation().getY() : null)")
    @Mapping(target = "imageUrl", ignore = true)
    @Mapping(target = "restaurantId", source = "restaurant.id")
    @Mapping(target = "restaurantName", source = "restaurant.name")
    @Mapping(target = "tags", source = "branchTags")
    @Mapping(target = "distance", ignore = true)
    public abstract BranchDto.BranchMapResponse toMapResponse(Branch entity);

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
    protected void addImageUrlToMap(Branch entity, @MappingTarget BranchDto.BranchMapResponse.BranchMapResponseBuilder builder) {
        mediaRepository.findPrimaryImageUrlByEntityId(entity.getId())
                .ifPresent(builder::imageUrl);
    }

    @AfterMapping
    protected void addMedias(Branch item, @MappingTarget BranchDto.BranchDetailResponse.BranchDetailResponseBuilder builder) {
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

    protected UUID map(BranchTagId branchTagId) {
        if (branchTagId == null) {
            return null;
        }
        return branchTagId.getTagId();
    }

    // Helper to extract Tag IDs from the composite key entities
    protected UUID map(BranchTag branchTag) {
        return (branchTag != null && branchTag.getId() != null) ? branchTag.getId().getTagId() : null;
    }

    // Helper for formatting price labels in both mapping types
    public String formatPriceRange(BigDecimal min, BigDecimal max) {
        if (min == null || max == null) return "Price N/A";
        return String.format("%d-%d₽", min.intValue(), max.intValue());
    }

    public BranchDto.TagResponse mapTag(BranchTag branchTag) {
        if (branchTag == null || branchTag.getTag() == null) return null;
        Tag t = branchTag.getTag();
        return new BranchDto.TagResponse(t.getId(), t.getName(), t.getIconUrl());
    }
}