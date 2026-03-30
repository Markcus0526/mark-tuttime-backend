package com.tuttime.restaurant.dto;

import lombok.*;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduleResponse {

	private UUID scheduleId;
//	private UUID parentId; // restaurantId or branchId depending on context
	private int dayOfWeek; // 0=Sunday ... 6=Saturday
	private LocalTime openTime;
	private LocalTime closeTime;
	private boolean isClosed;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

}
