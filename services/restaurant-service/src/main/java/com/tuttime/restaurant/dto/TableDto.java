package com.tuttime.restaurant.dto;

import com.tuttime.restaurant.enums.TableStatus;
import lombok.*;

import java.util.UUID;

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
	private TableStatus status;

}
