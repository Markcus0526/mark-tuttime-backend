package com.tuttime.pos.dto.rkeeper.booking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RKeeperBookingDTO {
	private String reservationId;
	private String clientName;
	private String clientPhone;
	private String restaurantCode;
	private String tableNumber;
	private String reservationStart;  // ISO 8601 string
	private String reservationEnd;    // ISO 8601 string
	private Integer personsCount;
}
