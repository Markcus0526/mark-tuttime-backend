package com.tuttime.order.repository;

import com.tuttime.order.entity.OrderItemModifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderItemModifierRepository extends JpaRepository<OrderItemModifier, UUID> {
	List<OrderItemModifier> findByOrderItemId(UUID orderItemId);
}
