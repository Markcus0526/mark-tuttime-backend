package com.tuttime.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuUpdatedEvent {
	private String restaurantId;
	private String posSystem;
	private Instant timestamp;
}