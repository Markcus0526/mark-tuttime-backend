package com.tuttime.booking.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.kafka.event.BookingEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_BOOKING_STATUS_UPDATED;
import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_PAYMENT_COMPLETED;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC_BOOKING_STATUS_UPDATED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingStatusUpdate(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            BookingEvent.BookingStatusUpdatedEvent event =
                    objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

            if (event.newStatus() == BookingStatus.CANCELED || event.newStatus() == BookingStatus.NO_SHOW) {
                log.info("Table freed at branch {} due to booking {} status {}",
                        event.branchId(), event.bookingId(), event.newStatus());
                bookingService.notifyNextWaiting(event.branchId(), event.zoneId(), event.partySize());
            }
        } catch (Exception e) {
            log.error("<<< KAFKA[{}] >>> Consumer:[booking-service], Error: {}",
                    TOPIC_PAYMENT_COMPLETED, e.getMessage());
            throw new RuntimeException("booking-service BookingEventConsumer failed to process " + TOPIC_PAYMENT_COMPLETED, e);
        }
    }
}