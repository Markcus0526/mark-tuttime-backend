package com.tuttime.analytics.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public interface HourlyBookingProjection {
	@Schema(example = "18", description = "The hour of the day (0-23)")
	Integer getHourOfDay();

	@Schema(example = "150", description = "Total bookings recorded in this hour")
	Long getCount();
}