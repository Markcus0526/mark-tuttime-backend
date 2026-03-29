package com.tuttime.restaurant.mapper;

import com.tuttime.restaurant.dto.MediaRequest;
import com.tuttime.restaurant.dto.MediaResponse;
import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.util.DateMapper;
import org.mapstruct.*;

@Mapper(componentModel = "spring", uses = DateMapper.class)
public interface MediaMapper {

	MediaResponse toResponse(Media entity);

	@Mapping(target = "mediaId", ignore = true)     // Primary Key (Auto-gen)
	@Mapping(target = "svgContent", ignore = true)  // Large BLOB or file content
	@Mapping(target = "isMap", ignore = true)       // Business logic flag
	@Mapping(target = "isDeleted", ignore = true)   // Soft delete
	@Mapping(target = "deletedAt", ignore = true)   // Soft delete timestamp
	@Mapping(target = "createdAt", ignore = true)   // JPA Auditing
	@Mapping(target = "updatedAt", ignore = true)   // JPA Auditing
	Media toEntity(MediaRequest request);

	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateEntityFromRequest(MediaRequest request, @MappingTarget Media entity);
}