package com.tuttime.pos.dto.liko.menu;

import com.tuttime.pos.dto.liko.order.IikoModifierDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoMenuItemDTO {
	private String menuItemId;
	private String name;
	private Double price;
	private List<IikoModifierDTO> modifiers;
}
