package com.tuttime.common.event.kafka.event;

import java.time.OffsetDateTime;
import java.util.UUID;

public sealed interface BaseEvent permits
		UserEvent.VerificationEvent,
		UserEvent.ProfileUpdatedEvent,
		UserEvent.SecurityEvent,

		RestaurantEvent.StaffUpdatedEvent,

		BookingEvent.BookingStatusUpdatedEvent,
		BookingEvent.BranchAvailabilityEvent,

		OrderEvent.OrderCreatedEvent,
		OrderEvent.OrderStatusEvent,

		PaymentEvent.PaymentCompletedEvent,
		PaymentEvent.PaymentRefundedEvent,

		PosEvent.PosMenuUpdatedEvent,
		PosEvent.PosPriceUpdatedEvent,

		ReviewEvent.ReviewApprovedEvent
{
	// Unique ID for idempotency (Critical for NotificationLog)
	UUID eventId();

	OffsetDateTime occurredAt();
}

