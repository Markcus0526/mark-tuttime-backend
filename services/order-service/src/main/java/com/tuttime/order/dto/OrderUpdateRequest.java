package com.tuttime.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record OrderUpdateRequest(
		@NotEmpty @Schema(description = "The complete list of items for the order")
		List<OrderItemUpdateRequest> items
) {}
