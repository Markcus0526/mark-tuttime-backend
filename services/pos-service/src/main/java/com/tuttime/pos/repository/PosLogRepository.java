package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PosLogRepository extends JpaRepository<PosLog, Long> {
	List<PosLog> findByEntityIdOrderByLastAttemptDesc(String entityId);

	// Helper to find PosLog by entityType and entityId (optional)
	PosLog findByEntityTypeAndEntityId(String entityType, String entityId);
}

