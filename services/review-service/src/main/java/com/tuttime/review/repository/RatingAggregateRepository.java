package com.tuttime.review.repository;

import com.tuttime.review.entity.RatingAggregate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RatingAggregateRepository extends JpaRepository<RatingAggregate, UUID> {

	/**
	 * Finds aggregate data for a specific restaurant.
	 * Used for the 'Restaurant Service' map-based search results.
	 */
	Optional<RatingAggregate> findByRestaurantId(UUID restaurantId);

	/**
	 * Efficiently checks if an aggregate record exists before recalculating.
	 */
	boolean existsByRestaurantId(UUID restaurantId);
}
