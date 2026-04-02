package com.tuttime.common.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class PaymentEvent {
	public record PaymentCompletedEvent(
			UUID bookingId,
			UUID userId,
			String stripePaymentIntentId,
			BigDecimal amount,
			String currency,
			Instant timestamp // Changed from long to Instant
	) implements BaseEvent {
	}

	/**
	 * Immutable event broadcasted via Kafka when a refund is confirmed.
	 * Distributed to Booking, Order, and Waitlist services.
	 */
	public record PaymentRefundedEvent(
			UUID bookingId,         // To identify which reservation to cancel
			UUID paymentId,         // For internal payment tracking
			BigDecimal amount,      // The amount refunded (Full or Partial)
			String currency,        // e.g., "USD"
			String stripeRefundId,  // For audit matching with Stripe
			String reason,          // Why the refund happened (e.g., "Customer Cancellation")
			Instant timestamp       // When the refund was processed
	) {
		// Convenience constructor for minimal data
		public PaymentRefundedEvent(UUID bookingId, BigDecimal amount, Instant timestamp) {
			this(bookingId, null, amount, "USD", null, "User Request", timestamp);
		}
	}
}