package com.tuttime.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Details of an individual synchronization attempt")
public record SyncLogDetail(

		@Schema(description = "Current status of this specific log", example = "FAILED")
		String status,

		@Schema(description = "How many times this sync has been attempted", example = "3")
		Integer attemptCount,

		@Schema(description = "Timestamp of the last synchronization attempt")
		OffsetDateTime lastAttempt,

		@Schema(description = "Error message or code if the status is FAILED", example = "POS_TERMINAL_OFFLINE")
		String errorMessage
) {
}
