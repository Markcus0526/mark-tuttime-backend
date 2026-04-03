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
public class RKeeperMenuDTO {
	private String restaurantCode;
	private List<RKeeperMenuCategoryDTO> menuCategories;
}

