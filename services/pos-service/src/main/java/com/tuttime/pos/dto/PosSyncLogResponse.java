package com.tuttime.pos.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "History of synchronization attempts")
public record PosSyncLogResponse(

		@Schema(description = "ID of the order/booking")
		UUID entityId,

		@Schema(description = "List of attempt details")
		List<SyncLogDetail> logs
) {
}
