package com.tuttime.booking.repository;

import com.tuttime.booking.entity.Booking;
import com.tuttime.common.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {

	@Query(value = """
        SELECT EXISTS (
            SELECT 1 FROM booking_tables bt
            JOIN bookings b ON bt.booking_id = b.id
            WHERE bt.table_id IN (:tableIds)
              AND b.status NOT IN ('CANCELED', 'COMPLETED')
              AND (b.booking_datetime, b.booking_datetime + (b.duration * INTERVAL '1 minute'))
                  OVERLAPS (:start, :end)
        )
        """, nativeQuery = true)
	boolean existsOverlappingBookingForTables(@Param("tableIds") List<UUID> tableIds,
	                                          @Param("start") OffsetDateTime start,
	                                          @Param("end") OffsetDateTime end);

	// -------------------- Existing methods (unchanged) --------------------
	List<Booking> findAllByUserIdOrderByBookingDatetimeDesc(UUID userId);

	@Query("SELECT b FROM Booking b WHERE b.status = :status AND b.createdAt < :cutoff")
	List<Booking> findAllByStatusAndCreatedAtBefore(BookingStatus status, OffsetDateTime cutoff);

	@Query("SELECT b FROM Booking b " +
			"WHERE b.branchId = :branchId " +
			"AND b.status != com.tuttime.common.enums.BookingStatus.CANCELED " +
			"AND b.bookingDatetime BETWEEN :start AND :end")
	List<Booking> findAllByBranchIdAndDayRange(
			@Param("branchId") UUID branchId,
			@Param("start") OffsetDateTime start,
			@Param("end") OffsetDateTime end);

	@Query(value = "SELECT * FROM bookings b WHERE b.status IN ('CREATED', 'PAID') " +
			"AND (b.booking_datetime + (b.grace_period_mins * INTERVAL '1 minute')) < :now",
			nativeQuery = true)
	List<Booking> findExpiredBookings(@Param("now") OffsetDateTime now);

	List<Booking> findByBookingDatetimeBetweenAndStatusAndReminderSendFalse(
			OffsetDateTime start,
			OffsetDateTime end,
			BookingStatus status
	);

	@Query("SELECT b.branchId, COUNT(b) FROM Booking b WHERE b.status IN ('CREATED','PAID','ARRIVED') GROUP BY b.branchId")
	List<Object[]> countActiveBookingsByBranch();

	@Query("SELECT COUNT(DISTINCT b.userId) FROM Booking b WHERE b.branchId = :branchId AND b.createdAt >= :startDate")
	int countFirstTimeCustomers(@Param("branchId") UUID branchId, @Param("startDate") OffsetDateTime startDate);

	@Query("SELECT COUNT(DISTINCT b.userId) FROM Booking b WHERE b.branchId = :branchId AND b.createdAt >= :startDate " +
			"AND b.userId IN (SELECT b2.userId FROM Booking b2 WHERE b2.branchId = :branchId AND b2.createdAt < :startDate)")
	int countRepeatCustomers(@Param("branchId") UUID branchId, @Param("startDate") OffsetDateTime startDate);

	@Query(value = "SELECT COALESCE(AVG(booking_count), 0) FROM (SELECT user_id, COUNT(*) AS booking_count " +
			"FROM bookings WHERE branch_id = :branchId AND created_at >= :startDate " +
			"GROUP BY user_id) AS stats", nativeQuery = true)
	Double averageOrdersPerCustomer(@Param("branchId") UUID branchId, @Param("startDate") OffsetDateTime startDate);

	@Query("SELECT COUNT(b.id) FROM Booking b WHERE b.branchId = :branchId AND DATE(b.createdAt) BETWEEN :startDate AND :endDate")
	long countBookings(@Param("branchId") UUID branchId, @Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

	@Query("SELECT COUNT(b.id) FROM Booking b WHERE b.branchId = :branchId AND DATE(b.updatedAt) BETWEEN :startDate AND :endDate")
	long countArrivals(@Param("branchId") UUID branchId, @Param("startDate") OffsetDateTime startDate, @Param("endDate") OffsetDateTime endDate);

	@Query("SELECT b.userId, SUM(b.totalAmount), COUNT(b.id) " +
			"FROM Booking b WHERE b.branchId = :branchId AND b.status IN ('COMPLETED', 'PAID') " +
			"GROUP BY b.userId ORDER BY SUM(b.totalAmount) DESC")
	Page<Object[]> findTopCustomersBySpending(@Param("branchId") UUID branchId, Pageable pageable);
}