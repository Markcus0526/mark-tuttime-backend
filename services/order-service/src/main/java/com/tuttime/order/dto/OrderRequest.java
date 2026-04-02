package com.tuttime.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Request object for creating a new order")
public record OrderRequest(
		@NotNull @Schema(description = "Associated booking ID", example = "550e8400-e29b-41d4-a716-446655440000")
		UUID bookingId,

		@NotNull @Schema(description = "ID of the user placing the order")
		UUID userId,

		@NotEmpty @Schema(description = "List of items in the order")
		List<OrderItemRequest> items
) {}