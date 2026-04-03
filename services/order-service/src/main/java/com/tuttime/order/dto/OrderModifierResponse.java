package com.tuttime.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderModifierResponse(
		UUID modifierId,
		BigDecimal priceAdjustment
) {
}