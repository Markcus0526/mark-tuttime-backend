package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Restaurant;

public interface RestaurantService {

	List<RestaurantResponse> getAllRestaurants();

	RestaurantResponse getRestaurantById(UUID id);

	RestaurantResponse createRestaurant(RestaurantRequest restaurant);

	RestaurantResponse updateRestaurant(UUID id, RestaurantRequest restaurantDetails);

	void deleteRestaurant(UUID id);
}
