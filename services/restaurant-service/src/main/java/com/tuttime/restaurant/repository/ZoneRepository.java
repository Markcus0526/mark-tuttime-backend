package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

	List<Zone> findAllByBranchIdAndDeletedFalse(UUID branchId);

	Optional<Zone> findByZoneIdAndDeletedFalse(UUID zoneId);

	@Modifying
	@Query("UPDATE Zone z SET z.deleted = true, z.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE z.branchId = :branchId AND z.deleted = false")
	void deleteAllByBranchId(@Param("branchId") UUID branchId);
}
