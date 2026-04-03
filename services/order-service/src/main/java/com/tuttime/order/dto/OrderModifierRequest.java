package com.tuttime.order.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderModifierRequest(
		@NotNull UUID modifierId,
		@NotNull BigDecimal priceAdjustment
) {
}
