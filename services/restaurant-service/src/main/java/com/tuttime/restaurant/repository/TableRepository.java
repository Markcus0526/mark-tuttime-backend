package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableRepository extends JpaRepository<Table, UUID> {

	// Find all tables in a given zone that are not soft-deleted
	List<Table> findAllByZoneIdAndIsDeletedFalse(UUID zoneId);

	// Find a specific table by ID and zone ID excluding soft-deleted entries
	Optional<Table> findByTableIdAndZoneIdAndIsDeletedFalse(UUID tableId, UUID zoneId);

	// Check if a table with the given number exists in the zone excluding soft-deleted entries
	boolean existsByZoneIdAndNumberAndIsDeletedFalse(UUID zoneId, String number);

	List<Table> findByZoneIdAndIsDeletedFalseAndIsClosedFalse(UUID zoneId);

	// Find a specific table by ID and status excluding soft-deleted entries
	Optional<Table> findByTableIdAndIsDeletedFalse(UUID tableId);

	@Query(value = """
        SELECT t.* FROM tables t
        WHERE t.branch_id = :branchId
        AND t.seats >= :seatSize
//        AND NOT EXISTS (
//            SELECT 1 FROM booking_service.bookings b
//            WHERE b.table_id = t.id
//            AND b.status NOT IN ('CANCELED', 'NO_SHOW')
//            -- Overlap Logic: (StartA < EndB) AND (EndA > StartB)
//            AND b.booking_datetime < :endTime
//            AND (b.booking_datetime + (b.duration * INTERVAL '1 minute')) > :startTime
//        )
    """, nativeQuery = true)
	List<Table> findAvailableTablesInBranch(
			@Param("branchId") UUID branchId,
			@Param("startTime") Instant startTime,
			@Param("endTime") Instant endTime,
			@Param("seatSize") Integer seatSize
	);
}
