package com.tuttime.order.dto;

import com.tuttime.common.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
		UUID id,
		UUID bookingId,
		UUID userId,
		OrderStatus status,
		BigDecimal totalAmount,
		List<OrderItemResponse> items,
		OffsetDateTime createdAt
) {
}
