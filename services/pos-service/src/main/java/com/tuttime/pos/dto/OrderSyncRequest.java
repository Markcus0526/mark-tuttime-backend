package com.tuttime.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// DTO for Order Sync Request
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderSyncRequest {
	private String orderId;
	private String posSystem;
}
