package com.tuttime.restaurant.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.restaurant.entity.enums.TableStatus;
import com.tuttime.restaurant.repository.TableRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
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
    @Transactional(readOnly = true)
    public void handleBookingStatusUpdated(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            BookingEvent.BookingStatusUpdatedEvent event =
                    objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

            List<UUID> tableIds;
            if (event.tableIds() != null && !event.tableIds().isEmpty()) {
                tableIds = event.tableIds();
            } else {
                log.warn("No table IDs in event: {}", event);
                return;
            }

            switch (event.newStatus()) {
                case PAID -> updateTableStatusBatch(tableIds, TableStatus.RESERVED);
                case ARRIVED -> updateTableStatusBatch(tableIds, TableStatus.OCCUPIED);
                case COMPLETED, CANCELED, NO_SHOW -> updateTableStatusBatch(tableIds, TableStatus.ACTIVE);
                default -> log.debug("No table status update needed for {}", event.newStatus());
            }

        } catch (Exception e) {
            log.error("<<< KAFKA[{}] >>> Consumer:[restaurant-service], Error: {}",
                    TOPIC_BOOKING_STATUS_UPDATED, e.getMessage());
            throw new RuntimeException("restaurant-service failed to process " + TOPIC_BOOKING_STATUS_UPDATED, e);
        }
    }

    private void updateTableStatusBatch(List<UUID> tableIds, TableStatus status) {
        if (tableIds == null || tableIds.isEmpty()) return;
        tableRepository.updateStatusByTableIds(tableIds, status);
    }
}