package com.tuttime.booking.dto;

import com.tuttime.common.enums.BookingStatus;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class BookingDto {

	/**
	 * Immutable DTO for creating a new booking.
	 */
	public record BookingRequest(
			@NotNull(message = "User ID is required")
			UUID userId,

			@NotNull(message = "Table ID is required")
			UUID tableId,

			@NotNull(message = "Booking time is required")
			@Future(message = "Booking must be in the future")
			OffsetDateTime bookingDatetime,

			@NotNull(message = "Duration is required")
			@Min(value = 15, message = "Minimum duration is 15 minutes")
			Integer duration,

			@NotNull(message = "Party size is required")
			@Min(value = 1, message = "Party size must be at least 1")
			Integer partySize,

			@NotNull(message = "Guest name is required for contact")
			String guestName,

			@NotNull(message = "Guest phone is required for the branch caller")
			String guestPhone,

			String specialRequest,

			// If a user books a table for 7:00 PM with a leadTimeMinutes of 15,
			// the system effectively marks that table as "in use" starting from 6:45 PM to ensure it's ready for the guest's arrival.
			Integer leadTimeMinutes,

			// If the booking is at 7:00 PM, the gracePeriodMins of 15 means the booking is technically valid until 7:15 PM.
			@Min(value = 0)
			Integer gracePeriodMins,

			List<PreOrderItemRequest> preOrderItems
	) {
		public record PreOrderItemRequest(
				@NotNull UUID menuItemId,
				@Min(1) Integer quantity,
				List<UUID> modifierIds, // For extras like "extra cheese"
				String notes
		) {
		}

		// Compact constructor for default values
		public BookingRequest {
			if (leadTimeMinutes == null) leadTimeMinutes = 0;
			if (gracePeriodMins == null) gracePeriodMins = 15;
		}
	}

	/**
	 * Immutable response DTO for client applications.
	 */
	public record BookingResponse(
			UUID bookingId,
			OffsetDateTime bookingDatetime,
			Integer duration,
			Integer partySize,
			Integer gracePeriodMins,
			BookingStatus status,
			BigDecimal totalAmount,
			String guestName,
			String guestPhone,
			String specialRequest,
			String cancellationReason,
			UserDetailResponse user
	) {
		public record UserDetailResponse(
				UUID userId,
				String phoneNumber,
				String email,
				String fullName
		) {
		}
	}

	public record BookingDetailResponse(
			UUID bookingId,
			OffsetDateTime bookingDatetime,
			Integer duration,
			Integer partySize,
			Integer gracePeriodMins,
			BookingStatus status,
			BigDecimal totalAmount,
			String cancellationReason,
			String guestName,
			String guestPhone,
			String specialRequest,
			UserDetailResponse user,
			TableDetailResponse table,
			List<MenuItemResponse> menuItems
	) {
		public record UserDetailResponse(
				UUID userId,
				String phoneNumber,
				String email,
				String fullName
		) {
		}

		public record TableDetailResponse(
				UUID branchId,
				UUID restaurantId,
				UUID zoneId,
				UUID tableId,
				String restaurantName,
				String branchName,
				String branchAddress,
				String branchPhone,
				String zoneName,
				String tableLabel
		) {
		}

		public record MenuItemResponse(
				UUID menuItemId,
				String itemName,      // From Menu gRPC
				Integer quantity,     // From local DB
				BigDecimal unitPrice, // From local DB (historical price)
				BigDecimal totalPrice,// Calculated (quantity * unitPrice)
				String notes          // From local DB
		) {
		}
	}

}
