package com.tuttime.booking.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityRequest {
	private Long branchId;
	private LocalDateTime slotStart; // 15min slot
	private int numGuests;
}
