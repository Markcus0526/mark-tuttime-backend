package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.DishTable;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.record.TableDetailRecord;
import com.tuttime.restaurant.repository.record.TableHierarchyRecord;
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
			    SELECT new com.tuttime.restaurant.repository.record.TableDetailRecord(
			             t.id,
			             z.id,
			             t.label,
			             z.name,
			             t.seats
			         )
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
	List<TableDetailRecord> findTablesByBranchIdAndPartySize(
			@Param("branchId") UUID branchId,
			@Param("partySize") int partySize,
			@Param("status") TableStatus status
	);

	@Modifying
	@Query("UPDATE DishTable t SET t.deleted = true, t.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE t.zone.id = :zoneId AND t.deleted = false")
	void deleteAllByZoneId(@Param("zoneId") UUID zoneId);

	@Query("""
			    SELECT new com.tuttime.restaurant.repository.record.TableHierarchyRecord(
				    t.id,
					z.id,
					z.branch.id,
			        b.restaurant.id, 
			        t.label,
				    z.name, 
			        b.name,
				    b.address,
					b.phone,
					r.name,
					CAST((SELECT COUNT(t2) FROM DishTable t2 WHERE t2.zone.branch.id = b.id AND t2.deleted = false) AS int)			
			    )
			    FROM DishTable t
			    JOIN t.zone z
			    JOIN z.branch b
				JOIN b.restaurant r
			    WHERE t.id = :tableId AND t.deleted = false
			""")
	Optional<TableHierarchyRecord> findTableHierarchy(@Param("tableId") UUID tableId);

}
