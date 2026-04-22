package com.tuttime.common.event.kafka;

public sealed interface BaseEvent permits
		UserEvent.ProfileUpdatedEvent,
		UserEvent.SecurityEvent,

		RestaurantEvent.StaffUpdatedEvent,

		BookingEvent.BookingStatusUpdatedEvent,

		OrderEvent.OrderCreatedEvent,
		OrderEvent.OrderStatusEvent,

		PaymentEvent.PaymentCompletedEvent,
		PaymentEvent.PaymentRefundedEvent,

		PosEvent.PosMenuUpdatedEvent,
		PosEvent.PosPriceUpdatedEvent,

		ReviewEvent.ReviewApprovedEvent
{}


