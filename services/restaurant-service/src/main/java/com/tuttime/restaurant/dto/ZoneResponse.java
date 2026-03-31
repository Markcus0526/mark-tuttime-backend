package com.tuttime.restaurant.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ZoneResponse {

	private UUID zoneId;
	private UUID branchId;
	private String name;
	private String description;
	private int capacity;
	private boolean isClosed;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

}
