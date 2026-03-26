package com.tuttime.review.mapper;

import com.tuttime.review.dto.RatingAggregateDto;
import com.tuttime.review.entity.RatingAggregate;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RatingAggregateMapper {

	RatingAggregateMapper INSTANCE = Mappers.getMapper(RatingAggregateMapper.class);

	RatingAggregateDto toDTO(RatingAggregate entity);

	RatingAggregate toEntity(RatingAggregateDto dto);
}