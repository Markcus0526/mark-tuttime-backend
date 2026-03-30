package com.tuttime.restaurant.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneDto {

	private UUID zoneId;
	private UUID branchId;
	private String name;
	private String description;
	private Integer capacity;

}
