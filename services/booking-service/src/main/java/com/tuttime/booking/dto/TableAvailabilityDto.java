package com.tuttime.booking.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableAvailabilityDto {
	private Long tableId;
	private String name;
	private int seats;
	private boolean available;
	private String lockedBy;
}
