package com.tuttime.pos.dto.liko.menu;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoMenuDTO {
	private String restaurantId;
	private List<IikoMenuCategoryDTO> categories;
}