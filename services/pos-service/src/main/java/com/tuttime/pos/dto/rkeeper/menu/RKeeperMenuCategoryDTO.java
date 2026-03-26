package com.tuttime.pos.dto.rkeeper.menu;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperMenuCategoryDTO {
	private String categoryCode;
	private String categoryName;
	private List<RKeeperMenuItemDTO> menuItems;
}
