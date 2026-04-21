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

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	// 1. Used in updateOrderContent to prevent race conditions
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT o FROM Order o WHERE o.id = :id")
	Optional<Order> findByIdWithLock(@Param("id") UUID id);

	// 2. The Dynamic Search Query
	@Query("SELECT o FROM Order o WHERE " +
			"(COALESCE(:branchId, NULL) IS NULL OR o.branchId = :branchId) AND " +
			"(COALESCE(:status, NULL) IS NULL OR o.status = :status) AND " +
			"o.deletedAt IS NULL")
	Page<Order> searchOrders(
			@Param("branchId") UUID branchId,
			@Param("status") OrderStatus status,
			Pageable pageable);

	// 3. User History Lookup
	@EntityGraph(attributePaths = {"items"})
	Page<Order> findByUserIdAndDeletedAtIsNull(UUID userId, Pageable pageable);

	// 4. Booking Lookup
	Optional<Order> findByBookingIdAndDeletedAtIsNull(UUID bookingId);

	@Query("SELECT o FROM Order o LEFT JOIN FETCH o.items WHERE o.bookingId = :bookingId AND o.deletedAt IS NULL")
	Optional<Order> findByBookingIdWithItems(@Param("bookingId") UUID bookingId);

}