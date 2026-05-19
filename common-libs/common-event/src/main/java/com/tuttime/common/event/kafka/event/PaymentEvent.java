package com.tuttime.common.event.kafka.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.PaymentStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class PaymentEvent {

    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record PaymentCompletedEvent(
            UUID eventId,
            UUID paymentId,
            UUID bookingId,
            UUID userId,
            UUID branchId,
            String externalPaymentId,
            BigDecimal amount,
            BigDecimal commissionRate,  // Add this: e.g., 0.10 for 10%
            String currency,
            PaymentStatus status,
            OffsetDateTime occurredAt
    ) implements BaseEvent {

        // Canonical Constructor to handle default values manually
        public PaymentCompletedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = OffsetDateTime.now();
        }

        @Override
        public UUID eventId() {
            return eventId;
        }

        @Override
        public OffsetDateTime occurredAt() {
            return occurredAt;
        }
    }

    /**
     * Immutable event broadcasted via Kafka when a refund is confirmed.
     * Distributed to Booking, Order, and Waitlist services.
     */
    @Builder(toBuilder = true)
    @JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
    public record PaymentRefundedEvent(
            UUID eventId,
            UUID paymentId,         // For internal payment tracking
            UUID bookingId,         // To identify which reservation to cancel
            UUID userId,
            UUID branchId,
            BigDecimal amount,      // The amount refunded (Full or Partial)
            String currency,        // e.g., "RUB"
            String stripeRefundId,  // For audit matching with Stripe
            String reason,          // Why the refund happened (e.g., "Customer Cancellation")
            OffsetDateTime occurredAt
    ) implements BaseEvent {

        // Canonical Constructor to handle default values manually
        public PaymentRefundedEvent {
            if (eventId == null) eventId = UUID.randomUUID();
            if (occurredAt == null) occurredAt = OffsetDateTime.now();
        }

        @Override
        public UUID eventId() {
            return eventId;
        }

        @Override
        public OffsetDateTime occurredAt() {
            return occurredAt;
        }
    }
}