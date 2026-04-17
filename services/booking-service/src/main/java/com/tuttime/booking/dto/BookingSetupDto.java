package com.tuttime.booking.dto;

import lombok.Builder;

import java.math.BigDecimal;
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

		@Builder
		public record MenuItemDto(
				UUID menuItemId,
				String name,
				BigDecimal price,
				String category,
				String description,
				List<ModifierGroupDto> modifierGroups
		) {}

		public record ModifierGroupDto(
				String name,
				int minSelect,
				int maxSelect,
				List<ModifierOptionDto> options
		) {}

		public record ModifierOptionDto(
				String name,
				BigDecimal extraPrice
		) {}
	}
}
