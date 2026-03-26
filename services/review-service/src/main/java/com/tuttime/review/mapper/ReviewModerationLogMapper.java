package com.tuttime.review.mapper;

import com.tuttime.review.dto.ReviewModerationLogDto;
import com.tuttime.review.entity.ReviewModerationLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ReviewModerationLogMapper {

	ReviewModerationLogMapper INSTANCE = Mappers.getMapper(ReviewModerationLogMapper.class);

	ReviewModerationLogDto toDTO(ReviewModerationLog entity);

	ReviewModerationLog toEntity(ReviewModerationLogDto dto);
}
