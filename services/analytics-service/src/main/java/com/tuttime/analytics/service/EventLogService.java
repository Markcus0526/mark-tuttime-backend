package com.tuttime.analytics.service;

import com.tuttime.analytics.dto.EventLogDto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface EventLogService {
	EventLogDto createEventLog(EventLogDto eventLogDto);
	EventLogDto getEventLogById(UUID id);
	List<EventLogDto> getEventLogsByTypeAndTimeRange(String eventType, OffsetDateTime start, OffsetDateTime end);
	void deleteEventLogById(UUID id);
}
