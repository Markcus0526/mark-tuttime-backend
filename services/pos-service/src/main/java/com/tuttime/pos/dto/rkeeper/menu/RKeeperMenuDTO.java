package com.tuttime.pos.dto.rkeeper.menu;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperMenuDTO {
	private String restaurantCode;
	private List<RKeeperMenuCategoryDTO> menuCategories;
}

