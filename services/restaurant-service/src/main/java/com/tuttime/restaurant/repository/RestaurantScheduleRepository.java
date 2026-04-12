package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.RestaurantSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantScheduleRepository extends JpaRepository<RestaurantSchedule, UUID> {

	Optional<RestaurantSchedule> findByScheduleIdAndDeletedFalse(UUID scheduleId);

	List<RestaurantSchedule> findAllByRestaurantIdAndDeletedFalse(UUID restaurantId);

	@Modifying
	@Query("UPDATE RestaurantSchedule r SET r.deleted = true, r.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE r.restaurantId = :restaurantId AND r.deleted = false")
	void deleteAllByRestaurantId(@Param("restaurantId") UUID restaurantId);

}