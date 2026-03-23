package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {

    Optional<Restaurant> findByEmailIgnoreCase(String email);

    Optional<Restaurant> findByPhone(String phone);

    /**
     * Check uniqueness constraints for user registration in a single query.
     * Returns a projection containing which fields already exist.
     *
     * <p>This method performs all uniqueness checks (email, phone, username) in one database query
     * instead of multiple sequential queries, significantly reducing latency.
     *
     * <p><b>Important:</b> Phone numbers are checked across ALL users (including deleted accounts)
     * to prevent re-registration with the same phone number. Email and username are only checked
     * for non-deleted accounts.
     *
     * @param email Email to check (required)
     * @param phone Phone to check (optional, can be null)
     * @param name Name to check (required)
     * @return UserUniquenessCheckProjection containing existence flags for each field
     */
    @Query(value = """
        SELECT 
            COUNT(CASE WHEN r.email = :email AND r.deleted_at IS NULL THEN 1 END) > 0 as emailExists,
            CASE 
                WHEN COALESCE(CAST(:phone AS VARCHAR), '') = '' THEN false
                ELSE COUNT(CASE WHEN r.phone = CAST(:phone AS VARCHAR) THEN 1 END) > 0
            END as phoneExists,
            COUNT(CASE WHEN LOWER(r.name) = LOWER(:name) AND r.deleted_at IS NULL THEN 1 END) > 0 as nameExists
        FROM restaurant_service.restaurants r
        WHERE (
            (r.deleted_at IS NULL AND r.email = :email) 
            OR (COALESCE(CAST(:phone AS VARCHAR), '') != '' AND r.phone = CAST(:phone AS VARCHAR))
            OR (r.deleted_at IS NULL AND LOWER(r.name) = LOWER(:name))
        )
        """, nativeQuery = true)
    RestaurantUniquenessCheckProjection checkRestaurantUniqueness(
            @Param("email") String email,
            @Param("phone") String phone,
            @Param("name") String name
    );

    @Query(value = """
    SELECT * FROM restaurant_service.restaurants r
    WHERE ST_DWithin(
        r.location, 
        ST_SetSRID(ST_MakePoint(:lon, :lat), 4326), 
        :distanceInMeters
    )
    ORDER BY ST_Distance(r.location, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
    """, nativeQuery = true)
    List<Restaurant> findNearby(
            @Param("lat") double lat,
            @Param("lon") double lon,
            @Param("distanceInMeters") double distance
    );
}
