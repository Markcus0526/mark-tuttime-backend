package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.Restaurant;

public interface RestaurantService {

	List<Restaurant> getAllRestaurants();

	Restaurant getRestaurantById(UUID id);

	Restaurant createRestaurant(Restaurant restaurant);

	Restaurant updateRestaurant(UUID id, Restaurant restaurantDetails);

	void deleteRestaurant(UUID id);
}
