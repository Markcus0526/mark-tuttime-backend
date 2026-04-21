package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosSyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PosSyncQueueRepository extends JpaRepository<PosSyncQueue, Integer> {

	// Finds all tasks that are due for a retry right now
	List<PosSyncQueue> findAllByStatusAndNextRetryAtBefore(String status, OffsetDateTime now);

	/**
	 * Perfect Native Query for your schema:
	 * 1. Joins pos_sync_queues -> pos_logs via pos_log_id
	 * 2. Joins pos_logs -> pos_configs via pos_system_id
	 * 3. Casts your VARCHAR(100) entity_id to UUID for the comparison
	 */
	@Query(value = "SELECT q.* FROM pos_service.pos_sync_queues q " +
			"JOIN pos_logs l ON q.pos_log_id = l.id " +
			"JOIN pos_configs c ON l.config_id = c.id " +
			"WHERE q.status = 'FAILED' " +
			"AND l.entity_id = CAST(:branchId AS VARCHAR)",
			nativeQuery = true)
	List<PosSyncQueue> findAllFailedByBranchId(@Param("branchId") UUID branchId);

	// Matches the field name 'posLog' in your Entity
	Optional<PosSyncQueue> findByPosLogId(Integer posLogId);

	// Optimized for the Scheduler: Get tasks that are ready for retry
	@Query("SELECT q FROM PosSyncQueue q WHERE q.status = 'PENDING' AND q.nextRetryAt <= :now")
	List<PosSyncQueue> findReadyForProcessing(@Param("now") OffsetDateTime now);
}
