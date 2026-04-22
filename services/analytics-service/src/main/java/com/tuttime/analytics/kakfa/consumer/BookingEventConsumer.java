package com.tuttime.analytics.kakfa.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.BookingEvent;
import com.tuttime.common.event.OrderEvent;
import com.tuttime.common.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.analytics.constants.AnalyticsConstants.*;
import static com.tuttime.common.event.EventProducer.TOPIC_BOOKING_STATUS_UPDATED;
import static com.tuttime.common.event.EventProducer.TOPIC_PAYMENT_COMPLETED;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventConsumer {

	private final ObjectMapper objectMapper;
	private final AnalyticsService analyticsService;

	/**
	 * Perfect Consumer Implementation for Spring Boot 3.3.13
	 * Processes 'BOOKING_CREATED' events for heatmaps and reporting.
	 */
	@KafkaListener(
			topics = TOPIC_BOOKING_STATUS_UPDATED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleBookingStatusUpdated(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			BookingEvent.BookingStatusUpdatedEvent event = objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

			log.info("Analytics received Booking Event: {} for Restaurant: {}",
					event.bookingId(), event.bookingId());

			String eventName = "";
			switch (event.newStatus()) {
				case BookingStatus.CREATED -> eventName = EVENT_BOOKING_CREATED;
				case BookingStatus.CANCELED -> eventName = EVENT_BOOKING_CANCELED;
				case BookingStatus.NO_SHOW -> eventName = EVENT_BOOKING_NO_SHOW;
				case BookingStatus.COMPLETED -> eventName = EVENT_BOOKING_COMPLETED;
			}
			// Mapping the specific event to the generic Analytics log
			if (StringUtils.hasText(eventName)) {
				analyticsService.processEvent(
						SOURCE_BOOKING_SERVICE,
						eventName,
						event.branchId(), // Important: Pass the branchId for physical tracking
						event);
			}
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[analytics-service], Error: {}", TOPIC_BOOKING_STATUS_UPDATED, e.getMessage());
		}
	}
}
