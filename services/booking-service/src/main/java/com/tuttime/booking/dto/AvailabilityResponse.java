package com.tuttime.booking.dto;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AvailabilityResponse {
	private List<TableAvailabilityDto> availableTables;
	private boolean slotCapacityOk;
}
