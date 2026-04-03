package com.tuttime.booking.repository;

import com.tuttime.booking.entity.WaitlistEntry;
import com.tuttime.booking.entity.enums.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WaitlistRepository extends JpaRepository<WaitlistEntry, UUID> {

	// Finds the next person in line for a specific branch/zone
	@Query("""
			    SELECT w FROM WaitlistEntry w 
			    WHERE w.branchId = :branchId 
			    AND w.status = 'WAITING' 
			    AND (:zoneId IS NULL OR w.zoneId = :zoneId)
			    ORDER BY w.joinedAt ASC
			""")
	List<WaitlistEntry> findPriorityQueue(@Param("branchId") UUID branchId,
	                                      @Param("zoneId") UUID zoneId);

	boolean existsByUserIdAndBranchIdAndStatus(UUID userId, UUID branchId, WaitlistStatus status);

	long countByBranchIdAndStatusAndJoinedAtBefore(UUID branchId, WaitlistStatus status, OffsetDateTime joinedAt);

	/**
	 * Finds the next person in line for a specific branch.
	 * Uses the 'joinedAt' timestamp to ensure First-Come, First-Served (FIFO).
	 */
	Optional<WaitlistEntry> findFirstByBranchIdAndStatusOrderByJoinedAtAsc(
			UUID branchId,
			WaitlistStatus status
	);

	/**
	 * Finds the next person in line for a specific branch AND a specific zone (e.g., VIP, Terrace).
	 */
	Optional<WaitlistEntry> findFirstByBranchIdAndZoneIdAndStatusOrderByJoinedAtAsc(
			UUID branchId,
			UUID zoneId,
			WaitlistStatus status
	);

}
