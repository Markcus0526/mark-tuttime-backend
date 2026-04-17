package com.tuttime.restaurant.repository.record;

import java.time.LocalTime;

/**
 * Internal record to map SQL results before converting to gRPC.
 */
public record ScheduleRecord(
		int dayOfWeek,
		LocalTime openTime,
		LocalTime closeTime,
		boolean closed
) {}
