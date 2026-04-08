package com.tuttime.booking.repository;

import com.tuttime.booking.entity.Booking;
import com.tuttime.booking.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {

	/**
	 *
	 * Checks if a table has any active bookings that overlap with the given time range.
	 * Logic: An overlap exists if (existingStart < newEnd) AND (existingEnd > newStart).
	 */
	@Query(value = "SELECT COUNT(*) > 0 FROM bookings b " +
			"WHERE b.table_id = :tableId " +
			"AND b.status NOT IN ('CANCELED', 'NO_SHOW') " +
			"AND b.booking_datetime < :endTime " +
			"AND (b.booking_datetime + (b.duration * INTERVAL '1 minute')) > :startTime",
			nativeQuery = true)
	boolean existsOverlappingBooking(
			@Param("tableId") UUID tableId,
			@Param("startTime") OffsetDateTime startTime,
			@Param("endTime") OffsetDateTime endTime
	);

	/**
	 * Bulk updates expired bookings (e.g., CREATED but not PAID within 15 mins).
	 * Uses JPQL for entity-state consistency.
	 */
	@Modifying
	@Query("""
			UPDATE Booking b 
			SET b.status = :newStatus, b.updatedAt = CURRENT_TIMESTAMP 
			WHERE b.status = :oldStatus 
			AND b.createdAt < :cutoff
			""")
	int cancelExpiredBookings(
			@Param("oldStatus") BookingStatus oldStatus,
			@Param("newStatus") BookingStatus newStatus,
			@Param("cutoff") OffsetDateTime cutoff
	);

	// Spring Data JPA derives the query: SELECT * FROM bookings WHERE user_id = :userId ORDER BY booking_datetime DESC
	List<Booking> findAllByUserIdOrderByBookingDatetimeDesc(UUID userId);
}
