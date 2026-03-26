package com.tuttime.review.service;

import com.tuttime.review.dto.RatingAggregateDto;

import java.util.UUID;

public interface RatingAggregateService {

	RatingAggregateDto getByRestaurantId(UUID restaurantId);

	RatingAggregateDto createOrUpdate(RatingAggregateDto dto);

	void delete(UUID restaurantId);
}