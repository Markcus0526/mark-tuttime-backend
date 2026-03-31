package com.tuttime.pos.dto.liko.booking;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IikoBookingDTO {
	private String bookingId;
	private String customerName;
	private String customerPhone;
	private String restaurantId;
	private String tableId;
	private String startTime;  // ISO 8601 string
	private String endTime;    // ISO 8601 string
	private Integer guests;
}

