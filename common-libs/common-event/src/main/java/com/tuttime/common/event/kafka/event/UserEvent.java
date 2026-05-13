package com.tuttime.common.event.kafka.event;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.enums.UserStatusEvent;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.tuttime.common.enums.VerificationType;

public class UserEvent {

	@Builder
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record VerificationEvent(
			UUID eventId,
			UUID userId,
			String phoneNumber,
			String email,
			String fullName,
			String verificationCode, // Optional: Only for verification events
			VerificationType type,
			NotificationTemplateKey templateKey, // Use Enum here
			Map<String, Object> parameters,
			OffsetDateTime occurredAt
	) implements BaseEvent {

		// Canonical Constructor to handle default values manually
		public VerificationEvent {
			if (eventId == null) eventId = UUID.randomUUID();
			if (occurredAt == null) occurredAt = OffsetDateTime.now();
		}

		@Override
		public UUID eventId() {
			return eventId;
		}

		@Override
		public OffsetDateTime occurredAt() {
			return occurredAt;
		}
	}

	@Builder(toBuilder = true)
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record SecurityEvent(
			UUID eventId,
			UUID userId,
			UserStatusEvent type, // "USER_BLOCKED", "USER_UNBLOCKED", "TOKEN_REVOKED"
			String jti,  // Only for TOKEN_REVOKED
			long ttlMillis,
			NotificationTemplateKey templateKey, // Use Enum here
			Map<String, Object> parameters,
			OffsetDateTime occurredAt
	) implements BaseEvent {

		// Canonical Constructor to handle default values manually
		public SecurityEvent {
			if (eventId == null) eventId = UUID.randomUUID();
			if (occurredAt == null) occurredAt = OffsetDateTime.now();
		}

		@Override
		public UUID eventId() {
			return eventId;
		}

		@Override
		public OffsetDateTime occurredAt() {
			return occurredAt;
		}
	}

	@Builder(toBuilder = true)
	@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
	public record ProfileUpdatedEvent(
			UUID eventId,
			UUID userId,
			String fullName,
			String profilePicture,
			String language,
			OffsetDateTime updatedAt,
			OffsetDateTime occurredAt
	) implements BaseEvent {

		// Canonical Constructor to handle default values manually
		public ProfileUpdatedEvent {
			if (eventId == null) eventId = UUID.randomUUID();
			if (occurredAt == null) occurredAt = OffsetDateTime.now();
		}

		@Override
		public UUID eventId() {
			return eventId;
		}

		@Override
		public OffsetDateTime occurredAt() {
			return occurredAt;
		}
	}
}
