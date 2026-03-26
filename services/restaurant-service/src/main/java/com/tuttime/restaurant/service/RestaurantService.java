package com.tuttime.restaurant.service;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.RestaurantRepository;

import org.springframework.stereotype.Service;

@Service
public class RestaurantService {

	private final RestaurantRepository repository;

	public RestaurantService(RestaurantRepository repository) {
		this.repository = repository;
	}

	public List<Restaurant> getAllRestaurants() {
		return repository.findAll();
	}

	public Restaurant getRestaurantById(UUID id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id " + id));
	}

	public Restaurant createRestaurant(Restaurant restaurant) {
		return repository.save(restaurant);
	}

	public Restaurant updateRestaurant(UUID id, Restaurant restaurantDetails) {
		Restaurant restaurant = getRestaurantById(id);

		restaurant.setOwnerUserId(restaurantDetails.getOwnerUserId());
		restaurant.setName(restaurantDetails.getName());
		restaurant.setDescription(restaurantDetails.getDescription());
		restaurant.setAddress(restaurantDetails.getAddress());
		restaurant.setLocation(restaurantDetails.getLocation());
		restaurant.setPhone(restaurantDetails.getPhone());
		restaurant.setWebsite(restaurantDetails.getWebsite());
		restaurant.setRating(restaurantDetails.getRating());

		return repository.save(restaurant);
	}

	public void deleteRestaurant(UUID id) {
		Restaurant restaurant = getRestaurantById(id);
		repository.delete(restaurant);
	}
}
