package com.tuttime.common.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
	public static final String TOPIC_USER_PROFILE_UPDATED = "tuttime.user.profile.updated";
	public static final String TOPIC_USER_SECURITY_UPDATED = "tuttime.user.security.updated";

	public static final String TOPIC_RESTAURANT_STAFF_UPDATED = "tuttime.restaurant.staff.updated";

	public static final String TOPIC_BOOKING_STATUS_UPDATED = "tuttime.booking.status.updated";

	public static final String TOPIC_PAYMENT_COMPLETED = "tuttime.payment.completed";
	public static final String TOPIC_PAYMENT_REFUND = "tuttime.payment.refund";

	public static final String TOPIC_ORDER_CREATED = "tuttime.order.created";
	public static final String TOPIC_ORDER_STATUS_UPDATED = "tuttime.order.status.updated";

	public static final String TOPIC_POS_ITEMS_UPDATED = "tuttime.pos.items.updated";
	public static final String TOPIC_POS_PRICE_UPDATED = "tuttime.pos.price.updated";

	public static final String TOPIC_REVIEW_APPROVED = "tuttime.review.approved";

	// User Events Publisher
	public void sendUserUpdatedEvent(UserEvent.ProfileUpdatedEvent event) {
		sendEvent(TOPIC_USER_PROFILE_UPDATED, event.userId().toString(), event);
	}

	public void sendUserSecurityEvent(UserEvent.SecurityEvent event) {
		sendEvent(TOPIC_USER_SECURITY_UPDATED, event.userId().toString(), event);
	}

	// Restaurant Events Publisher
	public void sendRestaurantStaffUpdatedEvent(RestaurantEvent.StaffUpdatedEvent event) {
		sendEvent(TOPIC_RESTAURANT_STAFF_UPDATED, event.staffId().toString(), event);
	}

	// Booking Events Publisher
	public void sendBookingStatusUpdatedEvent(BookingEvent.BookingStatusUpdatedEvent event) {
		sendEvent(TOPIC_BOOKING_STATUS_UPDATED, event.bookingId().toString(), event);
	}


	// Payment Events Publisher
	public void sendPaymentCompletedEvent(PaymentEvent.PaymentCompletedEvent event) {
		sendEvent(TOPIC_PAYMENT_COMPLETED, event.bookingId().toString(), event);
	}

	public void sendPaymentRefundEvent(PaymentEvent.PaymentRefundedEvent event) {
		sendEvent(TOPIC_PAYMENT_REFUND, event.bookingId().toString(), event);
	}

	// Order Events Publisher
	public void sendOrderCreatedEvent(OrderEvent.OrderCreatedEvent event) {
		sendEvent(TOPIC_ORDER_CREATED, event.orderId().toString(), event);
	}

	public void sendOrderStatusUpdatedEvent(OrderEvent.OrderStatusEvent event) {
		sendEvent(TOPIC_ORDER_STATUS_UPDATED, event.bookingId().toString(), event);
	}

	// Pos Events Publisher
	public void sendPosMenuUpdatedEvent(PosEvent.PosMenuUpdatedEvent event) {
		sendEvent(TOPIC_POS_ITEMS_UPDATED, event.branchId().toString(), event);
	}

	public void sendPosPriceUpdatedEvent(PosEvent.PosPriceUpdatedEvent event) {
		sendEvent(TOPIC_POS_PRICE_UPDATED, event.branchId().toString(), event);
	}

	// Review Events Publisher
	public void sendReviewApprovedEvent(ReviewEvent.ReviewApprovedEvent event) {
		sendEvent(TOPIC_REVIEW_APPROVED, event.eventId().toString(), event);
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
