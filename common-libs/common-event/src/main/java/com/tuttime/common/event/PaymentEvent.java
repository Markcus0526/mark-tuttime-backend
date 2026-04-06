package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record PaymentCompletedEvent(
			UUID bookingId,
			UUID userId,
			String stripePaymentIntentId,
			BigDecimal amount,
			String currency,
			OffsetDateTime timestamp // Changed from long to Instant
	) implements BaseEvent {
	}

	/**
	 * Immutable event broadcasted via Kafka when a refund is confirmed.
	 * Distributed to Booking, Order, and Waitlist services.
	 */
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record PaymentRefundedEvent(
			UUID bookingId,         // To identify which reservation to cancel
			UUID paymentId,         // For internal payment tracking
			BigDecimal amount,      // The amount refunded (Full or Partial)
			String currency,        // e.g., "USD"
			String stripeRefundId,  // For audit matching with Stripe
			String reason,          // Why the refund happened (e.g., "Customer Cancellation")
			OffsetDateTime timestamp       // When the refund was processed
	) implements BaseEvent {
		// Convenience constructor for minimal data
		public PaymentRefundedEvent(UUID bookingId, BigDecimal amount, OffsetDateTime timestamp) {
			this(bookingId, null, amount, "USD", null, "User Request", timestamp);
		}
	}
}