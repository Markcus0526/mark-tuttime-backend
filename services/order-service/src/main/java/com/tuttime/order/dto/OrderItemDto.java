package com.tuttime.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
	private UUID id;
	private UUID menuItemId;
	private int quantity;
	private BigDecimal price;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;
	private List<OrderItemModifierDto> modifiers;

	// Getters and setters
}
