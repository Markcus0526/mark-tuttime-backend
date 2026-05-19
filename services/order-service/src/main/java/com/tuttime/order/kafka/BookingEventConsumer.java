package com.tuttime.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.kafka.event.BookingEvent;
import com.tuttime.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_BOOKING_STATUS_UPDATED;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingEventConsumer {

    private final ObjectMapper objectMapper;
    private final OrderService orderService;

    @KafkaListener(
            topics = TOPIC_BOOKING_STATUS_UPDATED,
            groupId = "${spring.kafka.consumer.group-id}")
    public void handleBookingStatusUpdated(String eventString) {
        try {
            if (!StringUtils.hasText(eventString)) return;

            BookingEvent.BookingStatusUpdatedEvent event =
                    objectMapper.readValue(eventString, BookingEvent.BookingStatusUpdatedEvent.class);

            // Handle CANCELLATIONS only
            if (event.newStatus() == BookingStatus.CANCELED) {
                orderService.cancelOrderByBookingId(event.bookingId());
            }

            // DO NOT handle PAID here. Let the Payment Consumer do it.
        } catch (Exception e) {
            log.error("<<< KAFKA[{}] >>> Consumer:[order-service], Error: {}",
                    TOPIC_BOOKING_STATUS_UPDATED, e.getMessage());
        }
    }
}