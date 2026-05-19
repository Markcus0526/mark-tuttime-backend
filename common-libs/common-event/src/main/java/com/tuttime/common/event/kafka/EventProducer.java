package com.tuttime.common.event.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.event.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static com.tuttime.common.event.kafka.TopicConstants.*;

@Slf4j
@Component
@RequiredArgsConstructor
//@ConditionalOnBean(KafkaTemplate.class)
public class EventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaMetrics kafkaMetrics;

    // User Events Publisher
    public void sendUserRegisteredEvent(UserEvent.VerificationEvent event) {
        sendEvent(TOPIC_USER_REGISTERED, event.eventId().toString(), event);
    }

    public void sendUserVerificationRequestedEvent(UserEvent.VerificationEvent event) {
        sendEvent(TOPIC_USER_VERIFICATION_REQUESTED, event.eventId().toString(), event);
    }

    public void sendUserUpdatedEvent(UserEvent.ProfileUpdatedEvent event) {
        sendEvent(TOPIC_USER_PROFILE_UPDATED, event.eventId().toString(), event);
    }

    public void sendUserSecurityEvent(UserEvent.SecurityEvent event) {
        sendEvent(TOPIC_USER_SECURITY_UPDATED, event.eventId().toString(), event);
    }

    // Restaurant Events Publisher
    public void sendRestaurantStaffUpdatedEvent(RestaurantEvent.StaffUpdatedEvent event) {
        sendEvent(TOPIC_RESTAURANT_STAFF_UPDATED, event.eventId().toString(), event);
    }

    // Booking Events Publisher
    public void sendBookingStatusUpdatedEvent(BookingEvent.BookingStatusUpdatedEvent event) {
        sendEvent(TOPIC_BOOKING_STATUS_UPDATED, event.eventId().toString(), event);
    }

    public void sendBookingBranchAvailabilityEvent(BookingEvent.BranchAvailabilityEvent event) {
        sendEvent(TOPIC_BRANCH_BRANCH_AVAILABILITY, event.eventId().toString(), event);
    }


    // Payment Events Publisher
    public void sendPaymentCompletedEvent(PaymentEvent.PaymentCompletedEvent event) {
        sendEvent(TOPIC_PAYMENT_COMPLETED, event.eventId().toString(), event);
    }

    public void sendPaymentRefundEvent(PaymentEvent.PaymentRefundedEvent event) {
        sendEvent(TOPIC_PAYMENT_REFUND, event.eventId().toString(), event);
    }

    // Order Events Publisher
    public void sendOrderCreatedEvent(OrderEvent.OrderCreatedEvent event) {
        sendEvent(TOPIC_ORDER_CREATED, event.eventId().toString(), event);
    }

    public void sendOrderStatusUpdatedEvent(OrderEvent.OrderStatusEvent event) {
        sendEvent(TOPIC_ORDER_STATUS_UPDATED, event.eventId().toString(), event);
    }

    // Pos Events Publisher
    public void sendPosMenuUpdatedEvent(PosEvent.PosMenuUpdatedEvent event) {
        sendEvent(TOPIC_POS_ITEMS_UPDATED, event.eventId().toString(), event);
    }

    public void sendPosPriceUpdatedEvent(PosEvent.PosPriceUpdatedEvent event) {
        sendEvent(TOPIC_POS_PRICE_UPDATED, event.eventId().toString(), event);
    }

    // Review Events Publisher
    public void sendReviewUpdatedEvent(ReviewEvent.ReviewUpdatedEvent event) {
        sendEvent(TOPIC_REVIEW_UPDATED, event.eventId().toString(), event);
    }

    private void sendEvent(String topic, String key, Object event) {
        try {
            ensureTimestamp(event);

            String message = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, message).whenComplete((result, ex) -> {
                boolean success = (ex == null);
                recordMetrics(topic, success, ex);

                if (success) {
                    log.info("<<< KAFKA[{}] >>> Published {}: event={}", topic, event.getClass().getSimpleName(), event);
                } else {
                    log.error("<<< KAFKA[{}] >>> Failed to publish {}: error={}, stack={}",
                            topic, event.getClass().getSimpleName(), ex.getMessage(), getStackTraceSummary(ex, 3));
                }
            });

        } catch (Exception e) {
            log.error("<<< KAFKA[{}] >>> Processing error: {}", topic, e.getMessage(), e);
            if (kafkaMetrics != null) kafkaMetrics.recordProducerError(topic, e.getClass().getSimpleName());
        }
    }

    /**
     * Replaces the messy if-else chain with polymorphic handling
     */
    @SuppressWarnings("unchecked")
    private void ensureTimestamp(Object event) {
        if (event instanceof Timestamped<?> t && t.getTimestamp() == null) {
            ((Timestamped<OffsetDateTime>) t).setTimestamp(OffsetDateTime.now(ZoneOffset.UTC));
        }
    }

    private void recordMetrics(String topic, boolean success, Throwable ex) {
        if (kafkaMetrics == null) return;
        kafkaMetrics.recordProducerMessageSent(topic, success);
        if (!success) {
            kafkaMetrics.recordProducerError(topic, ex.getClass().getSimpleName());
        }
    }

    private String getStackTraceSummary(Throwable ex, int maxLines) {
        if (ex == null || ex.getStackTrace() == null) return "";
        StackTraceElement[] st = ex.getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(maxLines, st.length); i++) {
            if (i > 0) sb.append(" | ");
            sb.append(st[i].toString());
        }
        return sb.toString();
    }
}
