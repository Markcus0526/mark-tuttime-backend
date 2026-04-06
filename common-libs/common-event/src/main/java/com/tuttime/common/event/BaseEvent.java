package com.tuttime.common.event;

public sealed interface BaseEvent permits
		UserEvent.SecurityEvent,

		MenuEvent.MenuUpdatedEvent,

		BookingEvent.BookingConfirmedEvent,
		BookingEvent.BookingCreatedEvent,

		OrderEvent.OrderCreatedEvent,
		OrderEvent.OrderStatusEvent,

		PaymentEvent.PaymentCompletedEvent,
		PaymentEvent.PaymentRefundedEvent,

		PosEvent.PosSyncedEvent,

		ReviewEvent.ReviewApprovedEvent
{}


