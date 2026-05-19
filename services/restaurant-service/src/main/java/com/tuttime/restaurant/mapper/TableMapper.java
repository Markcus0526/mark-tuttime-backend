package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaDto;
import com.tuttime.restaurant.dto.TableDto;
import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.repository.MediaRepository;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {MediaMapper.class})
public abstract class TableMapper {

    @Autowired
    protected MediaRepository mediaRepository;

    @Autowired
    protected MediaMapper mediaMapper;

    public abstract DishTable toEntity(TableDto.TableRequest request);

    @Mapping(target = "tableId", source = "id")
    @Mapping(target = "zoneId", source = "zone.id")
    public abstract TableDto.TableResponse toResponse(DishTable table);

    @Mapping(target = "tableId", source = "id")
    @Mapping(target = "zoneId", source = "zone.id")
    public abstract TableDto.TableDetailResponse toDetailResponse(DishTable table);


    public abstract void updateEntityFromRequest(TableDto.TableRequest request, @MappingTarget DishTable entity);

    @AfterMapping
    protected void addImageUrl(DishTable entity, @MappingTarget TableDto.TableResponse.TableResponseBuilder builder) {
        // Uses the logic from Page 9 of your PDF (entity_id + is_primary)
        mediaRepository.findPrimaryImageUrlByEntityId(entity.getId())
                .ifPresent(builder::imageUrl);
    }

    @AfterMapping
    protected void addMedias(DishTable item, @MappingTarget TableDto.TableDetailResponse.TableDetailResponseBuilder builder) {
        // Fetch ALL Media (Images & Videos)
        List<MediaDto.MediaResponse> allMedia = mediaRepository.findAllImageAndVideoByEntityId(item.getId())
                .stream()
                .map(mediaMapper::toResponse)
                .collect(Collectors.toList());

        builder.medias(allMedia);
    }
}

