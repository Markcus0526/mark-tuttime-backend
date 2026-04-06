package com.tuttime.common.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

public class UserEvent {

	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record SecurityEvent(
			String userId,
			String type, // "USER_BLOCKED", "USER_UNBLOCKED", "TOKEN_REVOKED"
			String jti,  // Only for TOKEN_REVOKED
			long ttlMillis // How long to keep this in Redis
	) implements BaseEvent {
	}
}
