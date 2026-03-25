package com.tuttime.menu.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategoryDto {
	private Long id;
	private Long branchId;
	private String name;
	private String description;
}