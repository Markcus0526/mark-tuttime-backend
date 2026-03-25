package com.tuttime.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemModifierDTO {
	private UUID id;
	private UUID modifierId;
	private BigDecimal priceAdjustment;
	private Instant createdAt;
	private Instant updatedAt;

	// Getters and setters
}