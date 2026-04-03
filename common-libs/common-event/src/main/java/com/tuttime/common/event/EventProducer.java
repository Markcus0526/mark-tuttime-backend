package com.tuttime.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
//@ConditionalOnBean(KafkaTemplate.class)
public class EventProducer {

	private final KafkaTemplate<String, String> kafkaTemplate;
	private final ObjectMapper objectMapper;
	private final KafkaMetrics kafkaMetrics;

	// Topics
	public static final String TOPIC_USER = "tuttime.user";
	public static final String TOPIC_RESTAURANT = "tuttime.restaurant";

	public static final String TOPIC_MENU_UPDATED = "tuttime.menu.updated";

	public static final String TOPIC_BOOKING_CONFIRMED = "tuttime.booking.confirmed";

	public static final String TOPIC_PAYMENT_COMPLETED = "tuttime.payment.completed";
	public static final String TOPIC_PAYMENT_REFUND = "tuttime.payment.refund";

	public static final String TOPIC_ORDER_CREATED = "tuttime.order.created";
	public static final String TOPIC_ORDER_STATUS_UPDATED = "tuttime.order.status.updated";

	public static final String TOPIC_POS_SYNCED = "tuttime.pos.synced";

	public static final String TOPIC_REVIEW_RATING_UPDATED = "tuttime.reiew.rating.synced";

	// Booking Events Publisher
	public void publishBookingConfirmedEvent(BookingEvent.BookingConfirmedEvent event) {
		publishEvent(TOPIC_BOOKING_CONFIRMED, event.restaurantId(), event);
	}

	// Payment Events Publisher
	public void publishPaymentCompletedEvent(PaymentEvent.PaymentCompletedEvent event) {
		publishEvent(TOPIC_PAYMENT_COMPLETED, event.bookingId().toString(), event);
	}

	public void publishPaymentRefundEvent(PaymentEvent.PaymentRefundedEvent event) {
		publishEvent(TOPIC_PAYMENT_REFUND, event.bookingId().toString(), event);
	}

	// Order Events Publisher
	public void publishOrderCreatedEvent(OrderEvent.OrderCreatedEvent event) {
		publishEvent(TOPIC_ORDER_CREATED, event.orderId(), event);
	}

	public void publishOrderStatusUpdatedEvent(OrderEvent.OrderStatusEvent event) {
		publishEvent(TOPIC_ORDER_STATUS_UPDATED, event.bookingId().toString(), event);
	}

	// Pos Events Publisher
	public void publishPosSyncedEvent(PosEvent.PosSyncedEvent event) {
		publishEvent(TOPIC_POS_SYNCED, event.entityId(), event);
	}

	// Review Events Publisher
	public void publishReviewRatingUpdatedEvent(ReviewEvent.RatingUpdateEvent event) {
		publishEvent(TOPIC_REVIEW_RATING_UPDATED, event.restaurantId().toString(), event);
	}

	private void publishEvent(String topic, String key, Object event) {
		try {
			ensureTimestamp(event);

			String message = objectMapper.writeValueAsString(event);
			kafkaTemplate.send(topic, key, message).whenComplete((result, ex) -> {
				boolean success = (ex == null);
				recordMetrics(topic, success, ex);

				if (success) {
					log.info("Published {} to {}: key={}", event.getClass().getSimpleName(), topic, key);
				} else {
					log.error("Failed to publish to {}: {}, stack={}", topic, ex.getMessage(), getStackTraceSummary(ex, 3));
				}
			});

		} catch (Exception e) {
			log.error("Processing error for topic {}: {}", topic, e.getMessage(), e);
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
