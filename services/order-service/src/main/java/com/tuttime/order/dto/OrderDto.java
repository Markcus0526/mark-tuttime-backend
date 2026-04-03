package com.tuttime.order.dto;

import com.tuttime.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	private List<OrderItemDto> orderItems;

	// Getters and setters
}