package com.tuttime.analytics.service.impl;

import com.tuttime.analytics.dto.EventResponse;
import com.tuttime.analytics.dto.HourlyBookingProjection;
import com.tuttime.analytics.dto.TopRestaurantProjection;
import com.tuttime.analytics.entity.SystemEvent;
import com.tuttime.analytics.repository.EventRepository;
import com.tuttime.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AnalyticsServiceImpl implements AnalyticsService {

	private final EventRepository eventRepository;

	/**
	 * Used by Kafka Consumers to persist incoming system events.
	 * Note: This is an internal support method.
	 */
	@Override
	@Transactional
	public void logEvent(String service, String type, UUID entityId, Object payload) {
		log.debug("Logging analytics event: {} from service: {}", type, service);

		SystemEvent event = SystemEvent.builder()
				.serviceName(service)
				.eventType(type)
				.entityId(entityId)
				.payload(payload)
				.occurredAt(OffsetDateTime.now())
				.build();

		eventRepository.save(event);
	}

	/**
	 * Retrieves all events associated with a specific restaurant.
	 * Maps Entity to Record for Controller response.
	 */
	@Override
	public List<EventResponse> getRestaurantHistory(UUID restaurantId) {
		return eventRepository.findByEntityIdOrderByOccurredAtDesc(restaurantId)
				.stream()
				.map(this::mapToResponse)
				.collect(Collectors.toList());
	}

	/**
	 * Calculates Top 10 Restaurants based on booking volume.
	 */
	@Override
	public List<TopRestaurantProjection> getTopRestaurants(int days) {
		OffsetDateTime since = OffsetDateTime.now().minusDays(days);
		return eventRepository.findTopRestaurantsByBookingCount(since);
	}

	/**
	 * Generates data for the Hourly Booking Heatmap (0-23).
	 */
	@Override
	public List<HourlyBookingProjection> getBookingHeatmap(int days) {
		OffsetDateTime since = OffsetDateTime.now().minusDays(days);
		return eventRepository.findBookingHeatmap(since);
	}

	/**
	 * Helper to map Entity to Record DTO.
	 */
	private EventResponse mapToResponse(SystemEvent e) {
		return new EventResponse(
				e.getId(),
				e.getServiceName(),
				e.getEventType(),
				e.getEntityId(),
				e.getPayload(),
				e.getOccurredAt()
		);
	}
}
