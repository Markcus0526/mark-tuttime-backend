package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Branch;
import com.tuttime.restaurant.repository.projection.BranchProjection;
import com.tuttime.restaurant.repository.record.BranchDetailRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BranchRepository extends JpaRepository<Branch, UUID>, JpaSpecificationExecutor<Branch> {

	Optional<Branch> findByIdAndDeletedFalse(UUID branchId);

	List<Branch> findAllByRestaurantIdAndDeletedFalse(UUID restaurantId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Transactional
	@Query("UPDATE Branch b SET b.deleted = true, b.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE b.restaurant.id = :restaurantId AND b.deleted = false")
	void deleteAllByRestaurantId(@Param("restaurantId") UUID restaurantId);

	@Query("""
			    SELECT new com.tuttime.restaurant.repository.record.BranchDetailRecord(
			        b.id, 
			        r.id, 
			        b.name, 
			        b.address, 
			        b.phone, 
			        r.name,
			        b.minPrice,
			        b.maxPrice
			    )
			    FROM Branch b
				LEFT JOIN b.restaurant r
				WHERE b.id = :branchId
			""")
	Optional<BranchDetailRecord> findBranchDetailById(@Param("branchId") UUID branchId);

	@Query(value = """
    SELECT 
        b.id as branchId, 
        b.restaurant_id as restaurantId, 
        b.name as branchName, 
        b.address as branchAddress, 
        b.phone as branchPhone, 
        r.name as restaurantName,
        ST_Distance(b.location, ST_SetSRID(ST_Point(:longitude, :latitude), 4326), true) as distanceInMeters,
        (SELECT COUNT(t.id) 
         FROM tables t 
         JOIN zones z ON t.zone_id = z.id
         WHERE z.branch_id = b.id 
           AND t.status = 'ACTIVE'::table_status_enum 
           AND t.closed = false 
           AND t.deleted = false
        ) as availableTables,
        b.min_price as minPrice, b.max_price as maxPrice
    FROM branches b
    JOIN restaurants r ON b.restaurant_id = r.id
    LEFT JOIN branch_tags bt ON b.id = bt.branch_id
    WHERE b.deleted = false
    AND ST_DWithin(b.location, ST_SetSRID(ST_Point(:longitude, :latitude), 4326), :radius)
    AND (:ignoreTags = true OR bt.tag_id = ANY(CAST(:tagIds AS uuid[])))
    AND (CAST(:query AS text) IS NULL OR b.name ILIKE CAST(:query AS text)) -- FIX: Added CAST
    GROUP BY b.id, r.id
    ORDER BY distanceInMeters ASC
    """,
			countQuery = """
    SELECT count(DISTINCT b.id) 
    FROM branches b
    LEFT JOIN branch_tags bt ON b.id = bt.branch_id
    WHERE b.deleted = false
    AND ST_DWithin(b.location, ST_SetSRID(ST_Point(:longitude, :latitude), 4326), :radius)
    AND (:ignoreTags = true OR bt.tag_id = ANY(CAST(:tagIds AS uuid[])))
    AND (CAST(:query AS text) IS NULL OR b.name ILIKE CAST(:query AS text))
    """,
			nativeQuery = true)
	Page<BranchProjection> findNearbyWithDistance(
			@Param("latitude") double latitude,
			@Param("longitude") double longitude,
			@Param("radius") double radiusMeters,
			@Param("tagIds") UUID[] tagIds,
			@Param("ignoreTags") boolean ignoreTags,
			@Param("query") String query,
			Pageable pageable);

	@Query("SELECT AVG(b.rating) FROM Branch b WHERE b.restaurant.id = :restaurantId AND b.deleted = false")
	Double getAverageRatingForRestaurant(@Param("restaurantId") UUID restaurantId);

	@Query("SELECT DISTINCT b FROM Branch b LEFT JOIN FETCH b.branchTags bt LEFT JOIN FETCH bt.tag WHERE b.id IN :ids")
	List<Branch> findAllByIdWithTags(@Param("ids") List<UUID> ids);

}