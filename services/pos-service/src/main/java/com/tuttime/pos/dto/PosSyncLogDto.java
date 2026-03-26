package com.tuttime.pos.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class PosSyncLogDto {
	private Long id;
	private Long restaurantId;
	private String entityId;
	private String status;
	private String message;
	private Instant syncTime;
}
