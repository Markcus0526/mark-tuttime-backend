package com.tuttime.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemDto {
	private UUID id;
	private UUID menuItemId;
	private int quantity;
	private BigDecimal price;
	private Instant createdAt;
	private Instant updatedAt;
	private List<OrderItemModifierDto> modifiers;

	// Getters and setters
}
