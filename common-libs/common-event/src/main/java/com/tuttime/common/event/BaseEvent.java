package com.tuttime.common.event;

public sealed interface BaseEvent permits
		PaymentEvent.PaymentCompletedEvent {}
