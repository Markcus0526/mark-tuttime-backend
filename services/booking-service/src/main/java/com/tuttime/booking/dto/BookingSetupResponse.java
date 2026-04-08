package com.tuttime.booking.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingSetupResponse(
		UUID branchId,
		List<AvailableSlot> slots,
		List<MenuItemDto> availableMenu
) {
	public record AvailableSlot(
			OffsetDateTime time,
			List<TableInfoDto> tables
	) {}

	public record TableInfoDto(
			UUID tableId,
			String tableNumber,
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
