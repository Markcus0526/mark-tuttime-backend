package com.tuttime.order.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderModifierDto {

    public record OrderModifierRequest(
            @NotNull UUID modifierId,
            @NotNull BigDecimal priceAdjustment
    ) {
    }

    public record OrderModifierResponse(
            UUID modifierId,
            BigDecimal priceAdjustment
    ) {
    }
}
