package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.projection.TableDetailProjection;
import com.tuttime.restaurant.repository.projection.TableHierarchyProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableRepository extends JpaRepository<DishTable, UUID> {

	// Find all tables in a given zone that are not soft-deleted
	List<DishTable> findAllByZoneIdAndDeletedFalse(UUID zoneId);

	// Check if a table with the given number exists in the zone excluding soft-deleted entries
	boolean existsByZoneIdAndLabelAndDeletedFalse(UUID zoneId, String number);

	List<DishTable> findAllByZoneIdAndDeletedFalseAndClosedFalse(UUID zoneId);

	// Find a specific table by ID and status excluding soft-deleted entries
	Optional<DishTable> findByIdAndDeletedFalse(UUID tableId);

	@Query("""
	    SELECT t.id AS tableId,
	           z.id AS zoneId,
	           t.label AS tableLabel,
	           z.name AS zoneName,
	           t.seats AS seats
	    FROM DishTable t
	    JOIN t.zone z
	    WHERE z.branch.id = :branchId 
	      AND t.status = :status
	      AND t.deleted = false 
	      AND t.closed = false 
	      AND t.seats >= :partySize
	      AND z.deleted = false
	      AND z.closed = false
	""")
	List<TableDetailProjection> findTablesByBranchIdAndPartySize(
			@Param("branchId") UUID branchId,
			@Param("partySize") int partySize,
			@Param("status") TableStatus status
	);

	@Modifying
	@Query("UPDATE DishTable t SET t.deleted = true, t.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE t.zone.id = :zoneId AND t.deleted = false")
	void deleteAllByZoneId(@Param("zoneId") UUID zoneId);

	@Query("""
	    SELECT t.id AS tableId,
	           z.id AS zoneId,
	           z.branch.id AS branchId,
	           b.restaurant.id AS restaurantId,
	           t.label AS tableLabel,
	           z.name AS zoneName,
	           b.name AS branchName,
	           b.address AS branchAddress,
	           b.phone AS branchPhone,
	           r.name AS restaurantName,
	           (SELECT COUNT(t2) FROM DishTable t2 WHERE t2.zone.branch.id = b.id AND t2.deleted = false) AS totalTables
	    FROM DishTable t
	    JOIN t.zone z
	    JOIN z.branch b
	    JOIN b.restaurant r
	    WHERE t.id = :tableId AND t.deleted = false
	""")
	Optional<TableHierarchyProjection> findTableHierarchy(@Param("tableId") UUID tableId);

	@Query("SELECT t FROM DishTable t " +
			"WHERE t.zone.branch.id = :branchId " +
			"AND (:zoneId IS NULL OR t.zone.id = :zoneId) " +
			"AND t.seats >= :partySize " +
			"AND t.deleted = false AND t.closed = false")
	List<DishTable> findEligibleTables(@Param("branchId") UUID branchId,
	                                   @Param("zoneId") UUID zoneId,
	                                   @Param("partySize") int partySize);

	@Query("SELECT COUNT(t) FROM DishTable t WHERE t.zone.branch.id = :branchId AND t.deleted = false AND t.closed = false")
	int countByBranchId(@Param("branchId") UUID branchId);

	@Modifying
	@Query("UPDATE DishTable t SET t.status = :status WHERE t.id IN :tableIds")
	void updateStatusByTableIds(@Param("tableIds") List<UUID> tableIds, @Param("status") TableStatus status);
}
