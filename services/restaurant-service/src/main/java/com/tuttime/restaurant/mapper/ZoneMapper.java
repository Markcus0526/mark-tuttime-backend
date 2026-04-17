package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.dto.ZoneDto;
import com.tuttime.restaurant.entity.Zone;
import com.tuttime.restaurant.repository.MediaRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
		unmappedTargetPolicy = ReportingPolicy.IGNORE,
		uses = {MediaMapper.class})
public abstract class ZoneMapper {

	@Autowired
	protected MediaRepository mediaRepository;

	@Autowired
	protected MediaMapper mediaMapper;

	public abstract Zone toEntity(ZoneDto.ZoneRequest request);

	@Mapping(target = "zoneId", source = "id")
	@Mapping(target = "branchId", source = "branch.id")
	@Mapping(target = "imageUrl", ignore = true)
	public abstract ZoneDto.ZoneResponse toResponse(Zone zone);

	@Mapping(target = "zoneId", source = "id")
	@Mapping(target = "branchId", source = "branch.id")
	@Mapping(target = "medias", ignore = true)
	public abstract ZoneDto.ZoneDetailResponse toDetailResponse(Zone zone);

	public abstract void updateEntityFromRequest(ZoneDto.ZoneRequest request, @MappingTarget Zone entity);

	@AfterMapping
	protected void addImageUrl(Zone entity, @MappingTarget ZoneDto.ZoneResponse.ZoneResponseBuilder builder) {
		// Uses the logic from Page 9 of your PDF (entity_id + is_primary)
		mediaRepository.findPrimaryImageUrlByEntityId(entity.getId())
				.ifPresent(builder::imageUrl);
	}

	@AfterMapping
	protected void addMedias(Zone item, @MappingTarget ZoneDto.ZoneDetailResponse.ZoneDetailResponseBuilder builder) {
		// Fetch ALL Media (Images & Videos)
		List<MediaDto.MediaResponse> allMedia = mediaRepository.findAllImageAndVideoByEntityId(item.getId())
				.stream()
				.map(mediaMapper::toResponse)
				.collect(Collectors.toList());

		builder.medias(allMedia);
	}
}

