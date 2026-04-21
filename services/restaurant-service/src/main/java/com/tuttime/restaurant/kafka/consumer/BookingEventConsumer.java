package com.tuttime.restaurant.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.BookingEvent;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.tuttime.common.event.EventProducer.TOPIC_BOOKING_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

	private final ObjectMapper objectMapper;
	private final TableRepository tableRepository;

	@KafkaListener(
			topics = TOPIC_BOOKING_STATUS_UPDATED,
			groupId = "restaurant-service-group")
	public void handleBookingStatusUpdated(String eventString) {
		BookingEvent.BookingStatusUpdatedEvent event = null;

		try {
			if (org.apache.commons.lang3.StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);
			BookingEvent.BookingStatusUpdatedEvent finalEvent = event;

			log.info("Received status change for booking {}: {} -> {}",
					finalEvent.bookingId(), finalEvent.oldStatus(), finalEvent.newStatus());

			UUID tableId = finalEvent.tableId();

			switch (finalEvent.newStatus()) {
				case PAID -> updateTableStatus(tableId, TableStatus.RESERVED);
				case ARRIVED -> updateTableStatus(tableId, TableStatus.OCCUPIED);
				case COMPLETED, CANCELED, NO_SHOW -> updateTableStatus(tableId, TableStatus.ACTIVE);
				default -> log.debug("No table status update needed for {}", finalEvent.newStatus());
			}

		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}

	private void updateTableStatus(UUID tableId, TableStatus status) {
		tableRepository.findById(tableId).ifPresent(table -> {
			table.setStatus(status);
			tableRepository.save(table);
			log.info("Table {} is now {}", tableId, status);
		});
	}
}