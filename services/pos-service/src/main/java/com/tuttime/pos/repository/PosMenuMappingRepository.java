package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosMenuMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosMenuMappingRepository extends JpaRepository<PosMenuMapping, Integer> {

	/**
	 * Finds the external POS ID (iiko/R-Keeper) for a specific item at a specific branch.
	 */
	@Query("SELECT m.externalPosId FROM PosMenuMapping m WHERE m.branchId = :branchId AND m.menuItemId = :menuItemId")
	Optional<String> findExternalIdByMenuItemId(UUID branchId, UUID menuItemId);

	/**
	 * Used during syncMenu to update or insert new mappings from the POS nomenclature.
	 */
	Optional<PosMenuMapping> findByBranchIdAndExternalPosId(UUID branchId, String externalPosId);

	@Modifying
	@Query("UPDATE PosMenuMapping m SET m.externalPrice = 0, m.externalName = CONCAT('[REMOVED] ', m.externalName) " +
			"WHERE m.branchId = :branchId AND m.lastSyncedAt < :syncTime")
	int deactivateOldMappings(UUID branchId, OffsetDateTime syncTime);

	/**
	 * Finds all TutTime menu_item_ids that were NOT updated in the last sync.
	 * These are items currently in our DB that the POS did not return.
	 */
	@Query("SELECT m.menuItemId FROM PosMenuMapping m " +
			"WHERE m.branchId = :branchId " +
			"AND m.lastSyncedAt < :syncTime " +
			"AND m.menuItemId IS NOT NULL")
	List<UUID> findMenuItemIdsByBranchAndLastSyncedBefore(
			@Param("branchId") UUID branchId,
			@Param("syncTime") OffsetDateTime syncTime
	);
}

