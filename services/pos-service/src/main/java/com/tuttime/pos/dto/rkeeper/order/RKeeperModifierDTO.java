package com.tuttime.pos.dto.rkeeper.order;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperModifierDTO {
	private String modifierCode;
	private Double priceChange;
}
