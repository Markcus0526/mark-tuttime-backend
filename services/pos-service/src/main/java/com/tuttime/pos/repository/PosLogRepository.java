package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PosLogRepository extends JpaRepository<PosLog, Integer> {

	// Used by getSyncLogs in the controller to show history
	List<PosLog> findAllByEntityIdOrderByCreatedAtDesc(UUID entityId);

	// Used to check if an order was already successfully synced (Idempotency)
	boolean existsByEntityIdAndStatus(UUID entityId, String status);
}
