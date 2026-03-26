package com.tuttime.review.mapper;

import com.tuttime.review.dto.RatingAggregateDTO;
import com.tuttime.review.entity.RatingAggregate;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface RatingAggregateMapper {

	RatingAggregateMapper INSTANCE = Mappers.getMapper(RatingAggregateMapper.class);

	RatingAggregateDTO toDTO(RatingAggregate entity);

	RatingAggregate toEntity(RatingAggregateDTO dto);
}