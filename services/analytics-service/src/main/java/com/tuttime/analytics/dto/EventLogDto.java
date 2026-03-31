package com.tuttime.analytics.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventLogDto {

	private UUID id;
	private String eventType;
	private String payload;
	private OffsetDateTime timestamp;
}
