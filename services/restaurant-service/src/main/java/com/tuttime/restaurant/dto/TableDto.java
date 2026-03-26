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
public class TableDto {

	private UUID tableId;
	private UUID zoneId;
	private String number;
	private Integer seats;
	private String status;

}
