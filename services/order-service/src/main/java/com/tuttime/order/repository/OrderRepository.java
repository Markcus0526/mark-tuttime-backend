package com.tuttime.order.repository;

import com.tuttime.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
	List<Order> findByBookingId(UUID bookingId);

	List<Order> findByUserId(UUID userId);
}
