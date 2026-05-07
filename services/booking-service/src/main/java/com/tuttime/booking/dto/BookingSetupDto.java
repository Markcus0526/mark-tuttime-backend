package com.tuttime.booking.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class BookingSetupDto {

	public record BookingSetupResponse(
			UUID branchId,
			List<BookingSetupResponse.AvailableSlot> slots
	) {
		public record AvailableSlot(
				OffsetDateTime time,
				boolean available,
				int tablesLeft,        // Provides "scarcity" UI cues (e.g., "Only 2 left!")
				List<String> zones     // e.g., ["Indoor", "Terrace"] - so user knows where they might sit
		) {
		}

		public record TableInfoDto(
				UUID tableId,
				String tableLabel,
				String zoneName,
				int capacity
		) {
		}

		@Builder
		public record MenuItemDto(
				UUID menuItemId,
				String name,
				BigDecimal price,
				String category,
				String description,
				List<ModifierGroupDto> modifierGroups
		) {
		}

		public record ModifierGroupDto(
				String name,
				int minSelect,
				int maxSelect,
				List<ModifierOptionDto> options
		) {
		}

		public record ModifierOptionDto(
				String name,
				BigDecimal extraPrice
		) {
		}
	}
}
