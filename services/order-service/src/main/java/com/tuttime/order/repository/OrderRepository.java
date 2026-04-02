package com.tuttime.order.repository;

import com.tuttime.order.entity.Order;
import jakarta.persistence.LockModeType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

	@EntityGraph(attributePaths = {"items", "items.modifiers"})
	Optional<Order> findById(@NonNull UUID id);

	@EntityGraph(attributePaths = {"items"})
	Optional<Order> findByBookingId(UUID bookingId);

	@Lock(LockModeType.PESSIMISTIC_WRITE) // Reserved for very high conflict scenarios
	@Query("SELECT o FROM Order o WHERE o.id = :id")
	Optional<Order> findByIdWithLock(UUID id);
}