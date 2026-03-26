package com.tuttime.pos.dto.liko.menu;

import com.tuttime.pos.dto.liko.order.IikoModifierDTO;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoMenuItemDTO {
	private String menuItemId;
	private String name;
	private Double price;
	private List<IikoModifierDTO> modifiers;
}
