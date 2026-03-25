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
public class OrderItemDTO {
	private UUID id;
	private UUID menuItemId;
	private int quantity;
	private BigDecimal price;
	private Instant createdAt;
	private Instant updatedAt;
	private List<OrderItemModifierDTO> modifiers;

	// Getters and setters
}
