package com.tuttime.restaurant.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.tuttime.restaurant.entity.Zone;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, UUID> {

	List<Zone> findByBranchIdAndIsDeletedFalse(UUID branchId);

	Optional<Zone> findByZoneIdAndBranchIdAndIsDeletedFalse(UUID zoneId, UUID branchId);

	Optional<Zone> findByZoneIdAndIsDeletedFalse(UUID branchId);
}
