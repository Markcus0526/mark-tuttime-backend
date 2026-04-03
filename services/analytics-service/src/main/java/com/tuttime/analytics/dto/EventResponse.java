package com.tuttime.analytics.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EventResponse(
		UUID id,
		String serviceName,
		String eventType,
		UUID entityId,
		Object payload,
		OffsetDateTime occurredAt
) {}
