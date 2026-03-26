package com.tuttime.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// DTO for Sync Log Response (aggregate of entries)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PosSyncLogResponse {
	private String entityId;
	private List<PosSyncLogEntry> logs;
}