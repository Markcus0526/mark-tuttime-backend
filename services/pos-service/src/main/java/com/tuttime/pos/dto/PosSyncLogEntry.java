package com.tuttime.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// DTO for Sync Log Entry
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PosSyncLogEntry {
	private String status;    // e.g., SUCCESS, FAILED
	private String message;
	private LocalDateTime timestamp;
}