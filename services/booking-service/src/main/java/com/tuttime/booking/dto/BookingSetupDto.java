package com.tuttime.booking.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class BookingSetupDto {

	public record BookingSetupResponse(
			UUID branchId,
			List<BookingSetupResponse.AvailableSlot> slots,
			List<BookingSetupResponse.MenuItemDto> availableMenu
	) {
		public record AvailableSlot(
				OffsetDateTime time,
				List<BookingSetupResponse.TableInfoDto> tables
		) {}

		public record TableInfoDto(
				UUID tableId,
				String tableLabel,
				String zoneName,
				int capacity
		) {}

		public record MenuItemDto(
				UUID menuItemId,
				String name,
				double price,
				String category
		) {}
	}
}
