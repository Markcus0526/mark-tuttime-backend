package com.tuttime.pos.repository;

import com.tuttime.pos.entity.PosSyncQueue;
import org.springframework.data.jpa.repository.JpaRepository;
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

	Optional<PosSyncQueue> findByBranchIdAndBookingId(@Param("branchId") UUID branchId, @Param("bookingId") UUID bookingId);

}
