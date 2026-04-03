package com.tuttime.order.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
		UUID id,
		UUID menuItemId,
		Integer quantity,
		BigDecimal price,
		List<OrderModifierResponse> modifiers
) {
}