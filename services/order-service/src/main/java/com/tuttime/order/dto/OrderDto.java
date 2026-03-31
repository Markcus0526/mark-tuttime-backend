package com.tuttime.order.dto;

import com.tuttime.order.enums.OrderStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
	private UUID id;
	private UUID bookingId;
	private UUID userId;
	private OrderStatus status;
	private BigDecimal totalAmount;
	private Instant createdAt;
	private Instant updatedAt;
	private List<OrderItemDto> orderItems;

	// Getters and setters
}