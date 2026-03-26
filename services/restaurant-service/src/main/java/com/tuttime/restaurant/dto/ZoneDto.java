package com.tuttime.restaurant.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
