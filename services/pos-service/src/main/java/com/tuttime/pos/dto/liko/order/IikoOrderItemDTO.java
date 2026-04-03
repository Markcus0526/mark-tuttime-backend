package com.tuttime.pos.dto.liko.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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