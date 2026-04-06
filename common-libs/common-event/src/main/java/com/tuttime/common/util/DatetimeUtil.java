package com.tuttime.common.util;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.OffsetDateTime;

public class DatetimeUtil {

	public static Timestamp convertOffsetDatetimeToProtobuf(OffsetDateTime offsetDateTime) {
		// Convert to Instant to get UTC epoch seconds and nanoseconds
		Instant instant = offsetDateTime.toInstant();

		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}
}
