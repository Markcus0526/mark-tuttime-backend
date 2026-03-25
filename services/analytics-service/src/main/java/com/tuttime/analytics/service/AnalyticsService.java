package com.tuttime.analytics.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.analytics.entity.Event;
import com.tuttime.analytics.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
	private final EventRepository eventRepository;
	private final ObjectMapper objectMapper;

	@KafkaListener(topics = {"order-events", "booking-events"}, groupId = "analytics-group")
	@SneakyThrows
	public void handleEvent(Map<String, Object> eventData) {
		Event event = new Event();
		event.setType((String) eventData.get("type"));
		event.setRestaurantId(((Number) eventData.getOrDefault("restaurantId", 0)).longValue());
		event.setBookingId(((Number) eventData.getOrDefault("bookingId", 0)).longValue());
		event.setOrderId(((Number) eventData.getOrDefault("orderId", 0)).longValue());
		event.setDataJson(objectMapper.writeValueAsString(eventData));
		eventRepository.save(event);
	}
}
