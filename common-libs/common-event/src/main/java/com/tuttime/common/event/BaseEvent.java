package com.tuttime.common.event;

public sealed interface BaseEvent permits
		MenuEvent.MenuUpdatedEvent,

		BookingEvent.BookingConfirmedEvent,

		OrderEvent.OrderCreatedEvent,
		OrderEvent.OrderStatusEvent,

		PaymentEvent.PaymentCompletedEvent,
		PaymentEvent.PaymentRefundedEvent,

		PosEvent.PosSyncedEvent,

		ReviewEvent.RatingUpdateEvent
{}


