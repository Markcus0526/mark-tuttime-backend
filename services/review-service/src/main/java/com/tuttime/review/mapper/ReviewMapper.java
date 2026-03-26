package com.tuttime.review.mapper;

import com.tuttime.review.dto.ReviewDTO;
import com.tuttime.review.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ReviewMapper {

	ReviewMapper INSTANCE = Mappers.getMapper(ReviewMapper.class);

	ReviewDTO toDTO(Review review);

	Review toEntity(ReviewDTO dto);
}