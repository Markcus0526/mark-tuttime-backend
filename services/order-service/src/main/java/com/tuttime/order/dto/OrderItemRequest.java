package com.tuttime.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record OrderItemRequest(
		@NotNull UUID menuItemId,
		@NotNull @Positive Integer quantity,
		List<OrderModifierRequest> modifiers
) {
}

