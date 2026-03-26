package com.tuttime.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuSyncRequest {
	private String restaurantId;
	private String posSystem;
}
