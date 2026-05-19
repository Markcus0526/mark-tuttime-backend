package com.tuttime.restaurant.service;

import com.tuttime.restaurant.dto.RestaurantDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RestaurantService {

    Page<RestaurantDto.RestaurantResponse> findRestaurants(String query, UUID ownerUserId, Double minRating, Double maxRating, Pageable pageable);

    RestaurantDto.RestaurantDetailResponse getRestaurantById(UUID restaurantId);

    RestaurantDto.RestaurantResponse createRestaurant(RestaurantDto.RestaurantRequest request, UUID currentUserId, String userRoles);

    RestaurantDto.RestaurantResponse updateRestaurant(UUID restaurantId, RestaurantDto.RestaurantUpdateRequest request, UUID currentUserId, String userRoles);

    void deleteRestaurant(UUID restaurantId, UUID currentUserId, String userRoles);

}
