package com.tuttime.booking.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import static com.tuttime.common.event.EventProducer.TOPIC_PAYMENT_COMPLETED;
import static com.tuttime.common.event.EventProducer.TOPIC_PAYMENT_REFUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final ObjectMapper objectMapper;
	private final BookingService bookingService;

	/**
	 * Confirms the booking once payment is successful.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_COMPLETED,
			groupId = "booking-service-group")
	public void handlePaymentCompleted(String eventString) {
		PaymentEvent.PaymentCompletedEvent event = null;

		try {
			if (org.apache.commons.lang3.StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, PaymentEvent.PaymentCompletedEvent.class);
			PaymentEvent.PaymentCompletedEvent finalEvent = event;

			log.info("Kafka: Payment confirmed for booking {}", event.bookingId());

			bookingService.markAsPaid(finalEvent.bookingId(), finalEvent.userId(), finalEvent.stripePaymentIntentId());
		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}

	/**
	 * Cancels the booking and frees the table if a refund occurs.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_REFUND,
			groupId = "booking-service-group")
	public void handlePaymentRefunded(String eventString) {
		PaymentEvent.PaymentRefundedEvent event = null;

		try {
			if (org.apache.commons.lang3.StringUtils.isEmpty(eventString)) {
				log.error("Received null event");
				return;
			}

			event = objectMapper.readValue(eventString, PaymentEvent.PaymentRefundedEvent.class);
			PaymentEvent.PaymentRefundedEvent finalEvent = event;

			log.info("Kafka: Refund received for booking {}. Releasing table.", finalEvent.bookingId());

			bookingService.updateStatus(
					finalEvent.bookingId(),
					BookingStatus.CANCELED,
					"Automatic Cancellation: Refund Processed",
					null,
					null);

		} catch (Exception e) {
			log.error("Fatal error in Kafka Consumer: {}", e.getMessage(), e);
		}
	}
}
