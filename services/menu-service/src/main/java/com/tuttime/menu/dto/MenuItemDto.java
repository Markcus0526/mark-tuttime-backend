package com.tuttime.menu.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItemDto {
	private Long id;
	private Long categoryId;
	private String name;
	private String description;
	private BigDecimal price;
	private Boolean isAvailable;
}