package com.tuttime.restaurant.dto;

import com.tuttime.restaurant.enums.TableStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableResponse {

	private UUID tableId;
	private UUID zoneId;
	private String number;
	private int seats;
	private TableStatus status;
	private boolean isClosed;
	private OffsetDateTime createdAt;
	private OffsetDateTime updatedAt;

}