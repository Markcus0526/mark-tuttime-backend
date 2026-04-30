package com.tuttime.booking.repository;

import com.tuttime.booking.entity.Booking;
import com.tuttime.common.enums.BookingStatus;
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

	@Query(value = "SELECT EXISTS (" +
			"SELECT 1 FROM bookings " +
			"WHERE table_id = :tableId " +
			"AND status NOT IN ('CANCELED') " +
			"AND (booking_datetime, (booking_datetime + (duration || ' minutes')::interval)) " +
			"OVERLAPS (:start, :end))", nativeQuery = true)
	boolean existsOverlappingBooking(UUID tableId, OffsetDateTime start, OffsetDateTime end);


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

	List<Booking> findAllByBranchIdAndBookingDatetimeBetween(UUID branchId, OffsetDateTime startOfDay, OffsetDateTime endOfDay);

	List<Booking> findAllByStatusAndCreatedAtBefore(BookingStatus status, OffsetDateTime cutoff);

	/**
	 * Fetches all active bookings for a branch within a specific time range.
	 * Use this to get all bookings for a full day.
	 */
	@Query("SELECT b FROM Booking b " +
			"WHERE b.branchId = :branchId " +
			"AND b.status != com.tuttime.common.enums.BookingStatus.CANCELED " +
			"AND b.bookingDatetime BETWEEN :start AND :end")
	List<Booking> findAllByBranchIdAndDayRange(
			@Param("branchId") UUID branchId,
			@Param("start") OffsetDateTime start,
			@Param("end") OffsetDateTime end);

	@Query(value = "SELECT * FROM bookings b " +
			"WHERE b.status IN ('CREATED', 'PAID') " +
			"AND (b.booking_datetime + (b.grace_period_mins || ' minutes')::interval) < :now",
			nativeQuery = true)
	List<Booking> findExpiredBookings(@Param("now") OffsetDateTime now);

	List<Booking> findByBookingDatetimeBetweenAndStatusAndReminderSentFalse(
			OffsetDateTime start,
			OffsetDateTime end,
			BookingStatus status
	);

	/**
	 * Finds all unique branch IDs that currently have bookings in the specified statuses.
	 */
	@Query("SELECT DISTINCT b.branchId FROM Booking b WHERE b.status IN :statuses")
	List<UUID> findDistinctBranchIdsByStatusIn(@Param("statuses") List<BookingStatus> statuses);

	/**
	 * Counts the total number of bookings for a specific branch that match the specified statuses.
	 */
	long countByBranchIdAndStatusIn(UUID branchId, List<BookingStatus> statuses);
}
