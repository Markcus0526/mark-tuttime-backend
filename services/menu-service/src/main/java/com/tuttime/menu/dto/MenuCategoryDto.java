package com.tuttime.menu.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuCategoryDto {
	private Long id;
	private Long branchId;
	private String name;
	private String description;
}