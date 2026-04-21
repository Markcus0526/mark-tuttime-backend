package com.tuttime.booking.dto;

import com.tuttime.common.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BookingHistoryDto {

	public record BookingHistoryResponse(
			Long historyId,
			BookingStatus oldStatus,
			BookingStatus newStatus,
			UUID changedBy,
			OffsetDateTime changedAt,
			String reason
	) {
	}

}
