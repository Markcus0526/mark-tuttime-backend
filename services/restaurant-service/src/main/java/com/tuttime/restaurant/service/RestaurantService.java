package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.RestaurantRequest;
import com.tuttime.restaurant.dto.RestaurantResponse;
import com.tuttime.restaurant.entity.Restaurant;
import com.tuttime.restaurant.entity.StaffRequest;
import com.tuttime.restaurant.entity.StaffResponse;
import com.tuttime.restaurant.entity.StaffUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public interface RestaurantService {

	Page<RestaurantResponse> findRestaurants(String query, UUID ownerUserId, Double minRating, Double maxRating, Pageable pageable);

	RestaurantResponse getRestaurantById(UUID restaurantId);

	RestaurantResponse createRestaurant(RestaurantRequest request, UUID currentUserId, String userRoles);

	RestaurantResponse updateRestaurant(UUID restaurantId, RestaurantRequest request, UUID currentUserId, String userRoles);

	void deleteRestaurant(UUID restaurantId, UUID currentUserId, String userRoles);

	Page<RestaurantResponse> findRestaurantsNearLocation(double latitude, double longitude, double radiusMeters, int page, int size);
}
