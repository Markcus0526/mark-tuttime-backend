package com.tuttime.booking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {
	private Long userId;
	private Long restaurantId;
	private Long branchId;
	private Long tableId;
	private LocalDateTime bookingTime;
	private int numGuests;
	private int prepTime;
}
