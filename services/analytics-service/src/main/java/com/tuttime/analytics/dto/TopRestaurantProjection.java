package com.tuttime.analytics.dto;

import java.util.UUID;

public interface TopRestaurantProjection {
	UUID getRestaurantId();

	Long getBookingCount();
}