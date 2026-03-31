package com.tuttime.pos.dto.rkeeper.order;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperModifierDTO {
	private String modifierCode;
	private Double priceChange;
}
