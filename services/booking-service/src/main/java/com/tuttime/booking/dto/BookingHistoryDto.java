package com.tuttime.booking.dto;

import com.tuttime.booking.entity.BookingStatusHistory;
import com.tuttime.common.enums.BookingStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public class BookingHistoryDto {

	public record BookingHistoryResponse(
			Long id,
			BookingStatus oldStatus,
			BookingStatus newStatus,
			UUID changedBy,
			OffsetDateTime changedAt,
			String reason
	) {
		public static BookingHistoryResponse fromEntity(BookingStatusHistory entity) {
			return new BookingHistoryResponse(
					entity.getId(),
					entity.getOldStatus(),
					entity.getNewStatus(),
					entity.getChangedBy(),
					entity.getChangedAt(),
					entity.getReason()
			);
		}
	}

}
