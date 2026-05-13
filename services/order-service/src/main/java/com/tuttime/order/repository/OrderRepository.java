package com.tuttime.order.repository;

import com.tuttime.common.enums.OrderStatus;
import com.tuttime.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	// 1. Used in updateOrderContent to prevent race conditions
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT o FROM Order o WHERE o.id = :id")
	Optional<Order> findByIdWithLock(@Param("id") UUID id);

	@Query("SELECT o FROM Order o WHERE o.branchId = :branchId " +
			"AND (COALESCE(:status, null) IS NULL OR o.status = :status) " +
			"AND (COALESCE(:minAmount, null) IS NULL OR o.totalAmount >= :minAmount) " +
			"AND (COALESCE(:maxAmount, null) IS NULL OR o.totalAmount <= :maxAmount) " +
			"AND (COALESCE(:startDateTime, null) IS NULL OR o.createdAt >= :startDateTime) " +
			"AND (COALESCE(:endDateTime, null) IS NULL OR o.createdAt <= :endDateTime) " +
			"AND (COALESCE(:query, '') = '' OR " +
			"    LOWER(o.guestName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
			"    LOWER(o.guestPhone) LIKE LOWER(CONCAT('%', :query, '%'))) " +
			"AND o.deletedAt IS NULL")
	Page<Order> searchOrders(@Param("branchId") UUID branchId,
	                         @Param("query") String query,
	                         @Param("status") OrderStatus status,
	                         @Param("startDateTime") OffsetDateTime startDateTime,
	                         @Param("endDateTime") OffsetDateTime endDateTime,
	                         @Param("minAmount") BigDecimal minAmount,
	                         @Param("maxAmount") BigDecimal maxAmount,
	                         Pageable pageable);

	// 3. User History Lookup
	@EntityGraph(attributePaths = {"items"})
	Page<Order> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

	// 4. Booking Lookup
	Optional<Order> findByBookingIdAndDeletedAtIsNull(UUID bookingId);

	@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.bookingId = :bookingId AND o.deletedAt IS NULL")
	Optional<Order> findByBookingIdWithItems(@Param("bookingId") UUID bookingId);

	Page<Order> findByUserIdAndStatusAndDeletedAtIsNull(UUID userId, OrderStatus status, Pageable pageable);

	Page<Order> findByUserIdAndBranchIdAndDeletedAtIsNull(UUID userId, UUID branchId, Pageable pageable);

	Page<Order> findByUserIdAndBranchIdAndStatusAndDeletedAtIsNull(UUID userId, UUID branchId, OrderStatus status, Pageable pageable);

	@Query("SELECT oi.menuItemId, SUM(oi.quantity), SUM(oi.price * oi.quantity) " +
			"FROM Order o JOIN o.items oi " +
			"WHERE o.branchId = :branchId AND DATE(o.createdAt) BETWEEN :from AND :to " +
			"GROUP BY oi.menuItemId ORDER BY SUM(oi.quantity) DESC")
	Page<Object[]> findTopItemsByBranchAndDateRange(UUID branchId, OffsetDateTime from, OffsetDateTime to, Pageable pageable);

	@Query("SELECT DATE(o.createdAt), SUM(o.totalAmount), COUNT(o) " +
			"FROM Order o WHERE o.branchId = :branchId AND DATE(o.createdAt) BETWEEN :start AND :end " +
			"GROUP BY DATE(o.createdAt)")
	List<Object[]> getDailyRevenue(UUID branchId, OffsetDateTime start, OffsetDateTime end);

	@Query("SELECT EXTRACT(HOUR FROM o.createdAt), SUM(o.totalAmount), COUNT(o) " +
			"FROM Order o WHERE o.branchId = :branchId AND DATE(o.createdAt) = :date " +
			"GROUP BY EXTRACT(HOUR FROM o.createdAt)")
	List<Object[]> getHourlyRevenue(UUID branchId, OffsetDateTime date);

	@Query("SELECT oi.menuItemId, SUM(oi.price * oi.quantity), SUM(oi.quantity) " +
			"FROM Order o JOIN o.items oi " +
			"WHERE o.branchId = :branchId AND DATE(o.createdAt) BETWEEN :start AND :end " +
			"GROUP BY oi.menuItemId ORDER BY SUM(oi.price * oi.quantity) DESC")
	List<Object[]> getItemPerformance(UUID branchId, OffsetDateTime start, OffsetDateTime end);
}