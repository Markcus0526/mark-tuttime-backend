package com.tuttime.orderservice.service;

import com.tuttime.orderservice.dto.OrderStatusUpdate;
import com.tuttime.orderservice.dto.PreOrderRequest;
import com.tuttime.orderservice.entity.Order;
import com.tuttime.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final KafkaTemplate<String, Object> kafkaTemplate;

  public Order createPreOrder(PreOrderRequest request) {
    Order order = Order.builder()
        .bookingId(request.getBookingId())
        .status("CREATED")
        .items(request.getMenuItems().toString()) // stub
        .build();
    order = orderRepository.save(order);

    // Push notification stub
    kafkaTemplate.send("order-events", "order.created", order.getId());

    return order;
  }

  public Order updateStatus(Long orderId, OrderStatusUpdate update) {
    Order order = orderRepository.findById(orderId).orElseThrow();

    // Validate transitions
    if (!isValidTransition(order.getStatus(), update.getStatus())) {
      throw new IllegalArgumentException("Invalid status transition: " + order.getStatus() + " -> " + update.getStatus());
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

  private boolean isValidTransition(OrderStatus from, OrderStatus to) {
    // Define allowed transitions e.g. CREATED -> PAID -> SENT_TO_RESTAURANT -> ACCEPTED -> PREPARING -> READY -> SERVED | COMPLETED
    // CANCELED from any < READY, REFUNDED post PAID
    return true; // Stub full enum logic
  }

}
