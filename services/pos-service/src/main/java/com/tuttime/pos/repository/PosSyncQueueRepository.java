package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosLog;
import com.tuttime.pos.entity.PosSyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PosSyncQueueRepository extends JpaRepository<PosSyncQueue, Long> {
	List<PosSyncQueue> findByStatusAndNextRetryAtBefore(String status, Instant time);

	Optional<PosSyncQueue> findByPosLog(PosLog posLog);
}
