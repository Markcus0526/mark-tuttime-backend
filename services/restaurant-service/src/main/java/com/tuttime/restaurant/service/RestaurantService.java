package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

public interface RestaurantService {

	Page<RestaurantResponse> findRestaurantAll(UUID ownerUserId, Double minRating, Double maxRating, int page, int size);

	RestaurantResponse getRestaurantById(UUID restaurantId);

	RestaurantResponse createRestaurant(RestaurantRequest request);

	RestaurantResponse updateRestaurant(UUID restaurantId, RestaurantRequest request);

	void deleteRestaurant(UUID restaurantId);

	Page<RestaurantResponse> findRestaurantsNearLocation(double latitude, double longitude, double radiusMeters, int page, int size);
}
