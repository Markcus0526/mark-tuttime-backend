package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Media;
import com.tuttime.restaurant.entity.RestaurantSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantScheduleRepository extends JpaRepository<RestaurantSchedule, UUID> {

	Optional<RestaurantSchedule> findByScheduleIdAndIsDeletedFalse(UUID scheduleId);

	List<RestaurantSchedule> findByRestaurantIdAndIsDeletedFalse(UUID restaurantId);

}