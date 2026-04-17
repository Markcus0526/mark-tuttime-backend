package com.tuttime.common.event;

public sealed interface BaseEvent permits
		UserEvent.ProfileUpdatedEvent,
		UserEvent.SecurityEvent,

		RestaurantEvent.StaffUpdatedEvent,

		MenuEvent.MenuUpdatedEvent,

		BookingEvent.BookingStatusUpdatedEvent,
		BookingEvent.BookingConfirmedEvent,

		OrderEvent.OrderCreatedEvent,
		OrderEvent.OrderStatusEvent,

		PaymentEvent.PaymentCompletedEvent,
		PaymentEvent.PaymentRefundedEvent,

		PosEvent.PosSyncedEvent,

		ReviewEvent.ReviewApprovedEvent
{}


