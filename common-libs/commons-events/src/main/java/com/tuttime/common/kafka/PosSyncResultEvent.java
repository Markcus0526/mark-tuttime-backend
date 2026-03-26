package com.tuttime.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

// Event published by POS Service after sync attempt
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PosSyncResultEvent {
	private String entityId;
	private String entityType;  // BOOKING, ORDER, MENU
	private String posSystem;
	private String status;      // SUCCESS, FAILED
	private String message;
	private Instant timestamp;
}
