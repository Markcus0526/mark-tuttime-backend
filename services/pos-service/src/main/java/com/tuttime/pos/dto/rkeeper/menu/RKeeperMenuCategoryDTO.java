package com.tuttime.pos.dto.rkeeper.menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperMenuCategoryDTO {
	private String categoryCode;
	private String categoryName;
	private List<RKeeperMenuItemDTO> menuItems;
}
