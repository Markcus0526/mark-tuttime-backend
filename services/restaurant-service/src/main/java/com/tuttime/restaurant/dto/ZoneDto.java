package com.tuttime.restaurant.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneDto {

	private Long zoneId;
	private Long branchId;
	private String name;
	private String description;
	private Integer capacity;

}
