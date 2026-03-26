package com.tuttime.review.mapper;

import com.tuttime.review.dto.ReviewModerationLogDTO;
import com.tuttime.review.entity.ReviewModerationLog;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ReviewModerationLogMapper {

	ReviewModerationLogMapper INSTANCE = Mappers.getMapper(ReviewModerationLogMapper.class);

	ReviewModerationLogDTO toDTO(ReviewModerationLog entity);

	ReviewModerationLog toEntity(ReviewModerationLogDTO dto);
}
