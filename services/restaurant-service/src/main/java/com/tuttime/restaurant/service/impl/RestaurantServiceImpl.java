package com.tuttime.restaurant.service.impl;

import java.util.List;
import java.util.UUID;

import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.exception.ResourceNotFoundException;
import com.tuttime.restaurant.repository.RestaurantRepository;

import com.tuttime.restaurant.service.RestaurantService;
import org.springframework.stereotype.Service;

@Service
public class RestaurantServiceImpl implements RestaurantService {

    private final RestaurantRepository repository;

    public RestaurantServiceImpl(RestaurantRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<Restaurant> getAllRestaurants() {
        return repository.findAll();
    }

    @Override
    public Restaurant getRestaurantById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant not found with id " + id));
    }

    @Override
    public Restaurant createRestaurant(Restaurant restaurant) {
        return repository.save(restaurant);
    }

    @Override
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

    @Override
    public void deleteRestaurant(UUID id) {
        Restaurant restaurant = getRestaurantById(id);
        repository.delete(restaurant);
    }
}
