package com.tuttime.restaurant.repository.projection;

import java.time.LocalTime;

/**
 * Internal record to map SQL results before converting to gRPC.
 */
public interface ScheduleProjection {
	Integer getDayOfWeek();
	LocalTime getOpenTime();
	LocalTime getCloseTime();
	Boolean getClosed();
}
