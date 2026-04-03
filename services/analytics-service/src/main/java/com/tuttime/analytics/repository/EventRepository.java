package com.tuttime.analytics.repository;

import com.tuttime.analytics.dto.HourlyBookingProjection;
import com.tuttime.analytics.dto.TopRestaurantProjection;
import com.tuttime.analytics.entity.SystemEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<SystemEvent, UUID> {

	List<SystemEvent> findByEventTypeOrderByOccurredAtDesc(String eventType);

	List<SystemEvent> findByEntityId(UUID entityId);

	@Query(value = """
        SELECT entity_id as restaurantId, COUNT(*) as bookingCount
        FROM analytics_service.system_events
        WHERE event_type = 'BOOKING_CREATED'
        AND occurred_at >= :since
        GROUP BY entity_id
        ORDER BY bookingCount DESC
        LIMIT 10
        """, nativeQuery = true)
	List<TopRestaurantProjection> findTopRestaurantsByBookingCount(@Param("since") OffsetDateTime since);

	@Query(value = """
        SELECT EXTRACT(HOUR FROM occurred_at) as hourOfDay, COUNT(*) as count
        FROM analytics_service.system_events
        WHERE event_type = 'BOOKING_CREATED'
        AND occurred_at >= :since
        GROUP BY hourOfDay
        ORDER BY hourOfDay ASC
        """, nativeQuery = true)
	List<HourlyBookingProjection> findBookingHeatmap(@Param("since") OffsetDateTime since);

	/**
	 * Standard Spring Data JPA query derivation.
	 * SQL: SELECT * FROM system_events WHERE entity_id = ? ORDER BY occurred_at DESC
	 */
	List<SystemEvent> findByEntityIdOrderByOccurredAtDesc(UUID entityId);
}