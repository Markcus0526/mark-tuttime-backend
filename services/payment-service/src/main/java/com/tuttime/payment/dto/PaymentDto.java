package com.tuttime.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentDto {

	/**
	 * Immutable DTO for initiating a payment session.
	 * Used by REST controllers and internal service mapping.
	 */
	public record PaymentRequest(
			@NotNull(message = "Booking ID is required")
			UUID bookingId,

			@NotNull(message = "User ID is required")
			UUID userId,

			@NotNull(message = "Branch ID is required")
			UUID branchId,

			@NotNull(message = "Amount is required")
			@DecimalMin(value = "0.01", message = "Amount must be greater than zero")
			BigDecimal amount,

			@NotBlank(message = "Currency is required")
			String currency, // e.g., "RUB"

			String description // Optional: e.g., "Table Reservation for Branch A"
	) {
		// Compact constructor to set defaults if needed
		public PaymentRequest {
			if (currency == null || currency.isBlank()) {
				currency = "RUB";
			}
		}
	}
}
