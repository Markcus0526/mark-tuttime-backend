package com.tuttime.restaurant.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableDto {

	private Long tableId;
	private Long zoneId;
	private String number;
	private Integer seats;
	private String status;

}
