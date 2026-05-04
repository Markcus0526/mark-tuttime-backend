package com.tuttime.restaurant.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_BOOKING_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

	private final ObjectMapper objectMapper;
	private final TableRepository tableRepository;

	@KafkaListener(
			topics = TOPIC_BOOKING_STATUS_UPDATED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleBookingStatusUpdated(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			BookingEvent.BookingStatusUpdatedEvent event =
					objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

			UUID tableId = event.tableId();

			switch (event.newStatus()) {
				case PAID -> updateTableStatus(tableId, TableStatus.RESERVED);
				case ARRIVED -> updateTableStatus(tableId, TableStatus.OCCUPIED);
				case COMPLETED, CANCELED, NO_SHOW -> updateTableStatus(tableId, TableStatus.ACTIVE);
				default -> log.debug("No table status update needed for {}", event.newStatus());
			}

		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[restaurant-service], Error: {}",
					TOPIC_BOOKING_STATUS_UPDATED, e.getMessage());
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