package com.tuttime.review.service;

import com.tuttime.review.dto.RatingAggregateDTO;

import java.util.UUID;

public interface RatingAggregateService {

	RatingAggregateDTO getByRestaurantId(UUID restaurantId);

	RatingAggregateDTO createOrUpdate(RatingAggregateDTO dto);

	void delete(UUID restaurantId);
}