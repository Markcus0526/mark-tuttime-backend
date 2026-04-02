package com.tuttime.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record OrderItemResponse(
		UUID id,
		UUID menuItemId,
		Integer quantity,
		BigDecimal price,
		List<OrderModifierResponse> modifiers
) {}