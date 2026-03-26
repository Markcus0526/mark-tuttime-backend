package com.tuttime.analytics.service.impl;

import com.tuttime.analytics.dto.EventLogDto;
import com.tuttime.analytics.entity.EventLog;
import com.tuttime.analytics.mapper.EventLogMapper;
import com.tuttime.analytics.repository.EventLogRepository;
import com.tuttime.analytics.service.EventLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class EventLogServiceImpl implements EventLogService {

	private final EventLogRepository eventLogRepository;
	private final EventLogMapper eventLogMapper;

	public EventLogServiceImpl(EventLogRepository eventLogRepository, EventLogMapper eventLogMapper) {
		this.eventLogRepository = eventLogRepository;
		this.eventLogMapper = eventLogMapper;
	}

	@Override
	public EventLogDto createEventLog(EventLogDto eventLogDto) {
		EventLog entity = eventLogMapper.toEntity(eventLogDto);
		EventLog saved = eventLogRepository.save(entity);
		return eventLogMapper.toDTO(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public EventLogDto getEventLogById(UUID id) {
		return eventLogRepository.findById(id)
				.map(eventLogMapper::toDTO)
				.orElseThrow(() -> new RuntimeException("EventLog not found with id: " + id));
	}

	@Override
	@Transactional(readOnly = true)
	public List<EventLogDto> getEventLogsByTypeAndTimeRange(String eventType, OffsetDateTime start, OffsetDateTime end) {
		return eventLogRepository.findAll().stream() // Replace with custom query in real implementation
				.filter(e -> e.getEventType().equals(eventType)
				&& !e.getTimestamp().isBefore(start)
				&& !e.getTimestamp().isAfter(end))
                .map(eventLogMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public void deleteEventLogById(UUID id) {
		eventLogRepository.deleteById(id);
	}
}