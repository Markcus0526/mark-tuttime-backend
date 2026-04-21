package com.tuttime.pos.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.BookingEvent;
import com.tuttime.pos.dto.PosSyncDto;
import com.tuttime.pos.service.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.UUID;

import static com.tuttime.common.event.EventProducer.TOPIC_BOOKING_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

	private final ObjectMapper objectMapper;
	private final PosService posService;

	@KafkaListener(
			topics = TOPIC_BOOKING_STATUS_UPDATED,
			groupId = "pos-service-group")
	public void handleBookingStatusUpdated(String eventString) {
		BookingEvent.BookingConfirmedEvent event = null;

		try {
			// 1. Handle Null Event
			if (!StringUtils.hasText(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, BookingEvent.BookingConfirmedEvent.class);
			BookingEvent.BookingConfirmedEvent finalEvent = event;

			PosSyncDto.BookingSyncRequest request = new PosSyncDto.BookingSyncRequest(finalEvent.branchId());
			posService.syncBooking(request);

		} catch (Exception e) {
			// 5. Final safety net to prevent infinite retries of bad messages
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}