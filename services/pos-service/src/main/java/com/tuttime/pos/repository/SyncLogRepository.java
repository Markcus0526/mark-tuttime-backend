package com.tuttime.pos.repository;

import com.tuttime.pos.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Integer> {

	// Resolves: findByOrderId(orderId)
	Optional<SyncLog> findByOrderId(UUID orderId);

	// Resolves: your query for restaurant and time
	List<SyncLog> findByRestaurantIdAndCreatedAtAfterOrderByCreatedAtDesc(
			UUID restaurantId,
			OffsetDateTime lastSync
	);
}