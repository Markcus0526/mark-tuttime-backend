package com.tuttime.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record OrderItemUpdateRequest(
		@Schema(description = "Existing Item ID (null for new items)")
		UUID id,
		@NotNull UUID menuItemId,
		@NotNull Integer quantity,
		List<OrderModifierRequest> modifiers
) {}