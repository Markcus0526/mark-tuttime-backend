package com.tuttime.review.mapper;

import com.tuttime.common.proto.UserDetailRequest;
import com.tuttime.common.proto.UserDetailResponse;
import com.tuttime.review.dto.RatingDto.RatingAggregateResponse;
import com.tuttime.review.dto.ReviewDto;
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
	ReviewResponse toReviewResponse(Review entity);

	// Maps Review entity to ReviewResponse Record
	@Mapping(target = "reviewId", source = "entity.id")
	@Mapping(target = "status", source = "entity.status")
	@Mapping(target = "createdAt", source = "entity.createdAt")
	@Mapping(target = "user.userId", source = "user.userId")
	@Mapping(target = "user.fullName", source = "user.fullName")
	@Mapping(target = "user.profilePicture", source = "user.profilePicture")
	@Mapping(target = "user.email", source = "user.email")
	@Mapping(target = "user.phoneNumber", source = "user.phoneNumber")
	ReviewResponse toReviewDetailResponse(Review entity, UserDetailResponse user);

	// Maps RatingAggregate entity to Response Record
	RatingAggregateResponse toRatingResponse(RatingAggregate aggregate);
}
