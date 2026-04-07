package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface RestaurantService {

	Page<RestaurantResponse> findRestaurantAll(UUID ownerUserId, Double minRating, Double maxRating, int page, int size);

	RestaurantResponse getRestaurantById(UUID restaurantId);

	RestaurantResponse createRestaurant(RestaurantRequest request);

	RestaurantResponse updateRestaurant(UUID restaurantId, RestaurantRequest request);

	void deleteRestaurant(UUID restaurantId);

	Page<RestaurantResponse> findRestaurantsNearLocation(double latitude, double longitude, double radiusMeters, int page, int size);

	void validateRestaurantOwner(UUID restaurantId);
}
