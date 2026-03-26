package com.tuttime.common.kafka;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedEvent {
	private String orderId;
	private String posSystem;
	private Instant timestamp;
}
