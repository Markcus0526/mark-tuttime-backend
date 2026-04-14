package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.DishTable;
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
	boolean existsByZoneIdAndNumberAndDeletedFalse(UUID zoneId, String number);

	List<DishTable> findAllByZoneIdAndDeletedFalseAndClosedFalse(UUID zoneId);

	// Find a specific table by ID and status excluding soft-deleted entries
	Optional<DishTable> findByIdAndDeletedFalse(UUID tableId);

	@Query("SELECT t.id, t.number, t.seats, z.name " +
			"FROM DishTable t JOIN Zone z ON t.zone.id = z.id " +
			"WHERE z.branch.id = :branchId AND t.seats >= :partySize AND t.status = 'ACTIVE'")
	List<Object[]> findTablesWithZoneName(@Param("branchId") UUID branchId, @Param("partySize") int partySize);

	@Modifying
	@Query("UPDATE DishTable t SET t.deleted = true, t.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE t.zone.id = :zoneId AND t.deleted = false")
	void deleteAllByZoneId(@Param("zoneId") UUID zoneId);
}
