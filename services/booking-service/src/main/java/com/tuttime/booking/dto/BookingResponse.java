package com.tuttime.booking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingResponse {
	private Long id;
	private Long userId;
	private Long restaurantId;
	private Long branchId;
	private Long tableId;
	private String status;
	private LocalDateTime bookingTime;
	private int numGuests;
	private int prepTime;
}
