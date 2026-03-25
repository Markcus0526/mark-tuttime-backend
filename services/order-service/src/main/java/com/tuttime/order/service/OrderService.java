package com.tuttime.order.service;

import com.tuttime.order.dto.OrderStatusUpdate;
import com.tuttime.order.dto.PreOrderRequest;
import com.tuttime.order.entity.Order;
import com.tuttime.order.entity.OrderStatus;
import com.tuttime.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderService {

	private final OrderRepository orderRepository;
	private final KafkaTemplate<String, Object> kafkaTemplate;

	public Order createPreOrder(PreOrderRequest request) {
		Order order = new Order();
		order.setBookingId(request.getBookingId());
		order.setStatus(OrderStatus.CREATED);
		order.setItemsJson(request.getMenuItems().toString()); // stub
		order = orderRepository.save(order);

		// Push notification stub
		kafkaTemplate.send("order-events", "order.created", order.getId());

		return order;
	}

	public Order updateStatus(Long orderId, OrderStatusUpdate update) {
		Order order = orderRepository.findById(orderId).orElseThrow();

		// Validate transitions
		if (!isValidTransition(order.getStatus(), update.getStatus())) {
			throw new IllegalArgumentException(
					"Invalid status transition: " + order.getStatus() + " -> " + update.getStatus());
		}

		order.setStatus(OrderStatus.valueOf(update.getStatus()));
		order.setUpdatedAt(LocalDateTime.now());
		order.setCancelReason(update.getCancelReason());
		order = orderRepository.save(order);

		// Push + integration POS
		kafkaTemplate.send("order-events", "order.status." + update.getStatus(), Map.of("orderId", orderId));
		if ("SENT_TO_RESTAURANT".equals(update.getStatus()) || "CANCELED".equals(update.getStatus())) {
			// Trigger POS via integration-service
		}

		return order;
	}

	private boolean isValidTransition(OrderStatus from, String toStr) {
		OrderStatus to = OrderStatus.valueOf(toStr);
		// Define allowed transitions e.g. CREATED -> PAID -> SENT_TO_RESTAURANT ->
		// ACCEPTED -> PREPARING -> READY -> SERVED | COMPLETED
		// CANCELED from any < READY, REFUNDED post PAID
		return true; // Stub full enum logic
	}

}
