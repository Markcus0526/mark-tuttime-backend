package com.tuttime.pos.dto.rkeeper.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperModifierDTO {
	private String modifierCode;
	private Double priceChange;
}
