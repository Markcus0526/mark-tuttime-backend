package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

	Page<Restaurant> findByOwnerUserId(UUID ownerUserId, Pageable pageable);

	Page<Restaurant> findByRatingBetween(Double minRating, Double maxRating, Pageable pageable);

	Page<Restaurant> findByOwnerUserIdAndRatingBetween(UUID ownerUserId, Double minRating, Double maxRating, Pageable pageable);

	Optional<Restaurant> findByRestaurantIdAndIsDeletedFalse(UUID restaurantId);

	boolean existsByRestaurantIdAndOwnerUserIdAndIsDeletedFalse(UUID restaurantId, UUID ownerId);

	@Query(value = "SELECT * FROM restaurant_service.restaurants r " +
			"WHERE r.is_deleted = false AND " +
			"ST_DWithin(r.location, ST_MakePoint(:lng, :lat)::geography, :radius) " +
			"ORDER BY ST_Distance(r.location, ST_MakePoint(:lng, :lat)::geography) ASC",
			countQuery = "SELECT count(*) FROM restaurant_service.restaurants r " +
					"WHERE r.is_deleted = false AND " +
					"ST_DWithin(r.location, ST_MakePoint(:lng, :lat)::geography, :radius)",
			nativeQuery = true)
	Page<Restaurant> findByLocationNear(@Param("lat") double latitude,
	                                    @Param("lng") double longitude,
	                                    @Param("radius") double radiusMeters,
	                                    Pageable pageable);
}
