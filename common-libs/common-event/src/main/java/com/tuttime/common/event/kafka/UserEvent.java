package com.tuttime.common.event.kafka;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.UserStatusEvent;

import java.time.OffsetDateTime;
import java.util.UUID;

public class UserEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record SecurityEvent(
			UUID userId,
			UserStatusEvent type, // "USER_BLOCKED", "USER_UNBLOCKED", "TOKEN_REVOKED"
			String jti,  // Only for TOKEN_REVOKED
			long ttlMillis // How long to keep this in Redis
	) implements BaseEvent {
	}

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record ProfileUpdatedEvent(
			UUID userId,
			String fullName,
			String profilePicture,
			OffsetDateTime updatedAt
	) implements BaseEvent {
	}
}
