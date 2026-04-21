package com.tuttime.review.mapper;

import com.tuttime.review.dto.RatingDto.RatingAggregateResponse;
import com.tuttime.review.dto.ReviewDto.ReviewResponse;
import com.tuttime.review.entity.RatingAggregate;
import com.tuttime.review.entity.Review;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ReviewMapper {

	// Maps Review entity to ReviewResponse Record
	@Mapping(target = "reviewId", source = "id")
	ReviewResponse toReviewResponse(Review review);

	// Maps RatingAggregate entity to Response Record
	RatingAggregateResponse toRatingResponse(RatingAggregate aggregate);
}
