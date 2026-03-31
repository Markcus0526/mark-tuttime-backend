package com.tuttime.pos.dto.liko.menu;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoMenuDTO {
	private String restaurantId;
	private List<IikoMenuCategoryDTO> categories;
}