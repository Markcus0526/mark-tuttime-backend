package com.tuttime.common.event.util;

import com.google.protobuf.Timestamp;

import java.time.Instant;
import java.time.OffsetDateTime;

public class ProtoUtil {

	public static Timestamp convertOffsetDatetimeToProtobuf(OffsetDateTime offsetDateTime) {
		// Convert to Instant to get UTC epoch seconds and nanoseconds
		Instant instant = offsetDateTime.toInstant();

		return Timestamp.newBuilder()
				.setSeconds(instant.getEpochSecond())
				.setNanos(instant.getNano())
				.build();
	}
}
