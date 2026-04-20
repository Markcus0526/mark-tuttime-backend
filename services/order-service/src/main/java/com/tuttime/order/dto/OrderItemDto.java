package com.tuttime.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class OrderItemDto {

	public record OrderItemRequest(
			@NotNull UUID menuItemId,
			@NotNull @Positive Integer quantity,
			List<OrderModifierDto.OrderModifierRequest> modifiers
	) {
	}

	public record OrderItemUpdateRequest(
			@Schema(description = "Existing Item ID (null for new items)")
			UUID orderItemId,
			@NotNull UUID menuItemId,
			@NotNull Integer quantity,
			List<OrderModifierDto.OrderModifierRequest> modifiers
	) {
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
