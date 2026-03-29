package com.tuttime.restaurant.security;

import com.tuttime.restaurant.repository.RestaurantRepository;
import com.tuttime.restaurant.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("restaurantSecurity") // Important: bean name used in @PreAuthorize
@RequiredArgsConstructor
public class RestaurantSecurity {

	private final RestaurantService restaurantService;
	private final RestaurantRepository restaurantRepository;


	/**
	 * Checks if the user with given username is the owner of the restaurant with the given ID.
	 */
	public boolean isOwner(UUID restaurantId, UUID userId) {
		if (restaurantId == null || userId == null) {
			return false;
		}
		return restaurantRepository.existsByRestaurantIdAndOwnerUserIdAndIsDeletedFalse(restaurantId, userId);
	}
}