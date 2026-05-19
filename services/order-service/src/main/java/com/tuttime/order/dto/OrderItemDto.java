package com.tuttime.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class OrderItemDto {

    public record OrderItemRequest(
            @NotNull UUID menuItemId,
            @NotNull @Positive Integer quantity,
            List<OrderModifierDto.OrderModifierRequest> modifiers
    ) {
        // Compact constructor to ensure modifiers is never null
        public OrderItemRequest {
            if (modifiers == null) modifiers = Collections.emptyList();
        }
    }

    public record OrderItemUpdateRequest(
            @Schema(description = "Existing Item ID (null for new items)")
            UUID orderItemId,
            @NotNull UUID menuItemId,
            @NotNull Integer quantity,
            List<OrderModifierDto.OrderModifierRequest> modifiers
    ) {
        // Compact constructor to ensure modifiers is never null
        public OrderItemUpdateRequest {
            if (modifiers == null) modifiers = Collections.emptyList();
        }
    }

    public record OrderItemResponse(
            UUID orderItemId,
            UUID menuItemId,
            Integer quantity,
            BigDecimal price,
            List<OrderModifierDto.OrderModifierResponse> modifiers
    ) {
    }
}
