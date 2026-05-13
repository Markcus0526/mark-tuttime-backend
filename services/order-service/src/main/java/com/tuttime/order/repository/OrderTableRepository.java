package com.tuttime.order.repository;

import com.tuttime.order.entity.OrderTable;
import com.tuttime.order.entity.OrderTableId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderTableRepository extends JpaRepository<OrderTable, OrderTableId> {

	/**
	 * Find all table assignments for a specific booking.
	 * @param orderId the booking ID
	 * @return list of OrderTable entities
	 */
	List<OrderTable> findById_OrderId(UUID orderId);

	/**
	 * Check if a particular table is already assigned to a booking.
	 * @param orderId booking ID
	 * @param tableId table ID
	 * @return true if exists
	 */
	boolean existsById_OrderIdAndId_TableId(UUID orderId, UUID tableId);

	/**
	 * Delete all table assignments for a given booking.
	 * @param orderId the booking ID
	 */
	@Modifying
	@Transactional
	@Query("DELETE FROM OrderTable ot WHERE ot.id.orderId = :orderId")
	void deleteByOrderId(@Param("orderId") UUID orderId);

	/**
	 * Find all bookings that include a specific table (for availability checks).
	 * @param tableId the table ID
	 * @return list of OrderTable entries
	 */
	List<OrderTable> findById_TableId(UUID tableId);

	/**
	 * Count how many tables are assigned to a booking.
	 * @param orderId the booking ID
	 * @return count of tables
	 */
	long countById_OrderId(UUID orderId);
}