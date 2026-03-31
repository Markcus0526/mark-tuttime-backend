package com.tuttime.pos.dto.liko.order;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoOrderItemDTO {
	private String menuItemId;
	private Integer quantity;
	private Double price;
	private List<IikoModifierDTO> modifiers;
}