package com.tuttime.pos.dto.rkeeper.order;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperOrderItemDTO {
	private String productCode;
	private Integer quantity;
	private Double price;
	private List<RKeeperModifierDTO> modifiers;
}
