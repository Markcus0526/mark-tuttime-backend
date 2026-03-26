package com.tuttime.pos.dto.liko.menu;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoMenuDTO {
	private String restaurantId;
	private List<IikoMenuCategoryDTO> categories;
}