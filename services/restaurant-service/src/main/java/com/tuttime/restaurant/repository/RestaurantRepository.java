package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID>, JpaSpecificationExecutor<Restaurant> {

	Optional<Restaurant> findByIdAndDeletedFalse(UUID restaurantId);

	boolean existsByNameIgnoreCase(String name);

	boolean existsByPhone(String phone);

	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

	boolean existsByPhoneAndIdNot(String phone, UUID id);

}
