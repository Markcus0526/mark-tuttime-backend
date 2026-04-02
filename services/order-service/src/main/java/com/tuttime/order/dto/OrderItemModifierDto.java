package com.tuttime.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemModifierDto {
	private UUID id;
	private UUID modifierId;
	private BigDecimal priceAdjustment;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

	// Getters and setters
}