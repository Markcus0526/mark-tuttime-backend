package com.tuttime.booking.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.event.kafka.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_PAYMENT_COMPLETED;
import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_PAYMENT_REFUND;

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
			groupId = "${spring.kafka.consumer.group-id}")
	public void handlePaymentCompleted(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			PaymentEvent.PaymentCompletedEvent event =
					objectMapper.readValue(eventString, PaymentEvent.PaymentCompletedEvent.class);

			log.info("Kafka: Payment confirmed for booking {}", event.bookingId());

			bookingService.markAsPaid(event.bookingId(), event.userId(), event.externalPaymentId());
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[booking-service], Error: {}",
					TOPIC_PAYMENT_COMPLETED, e.getMessage());
		}
	}

	/**
	 * Cancels the booking and frees the table if a refund occurs.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_REFUND,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handlePaymentRefunded(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			PaymentEvent.PaymentRefundedEvent event =
					objectMapper.readValue(eventString, PaymentEvent.PaymentRefundedEvent.class);

			bookingService.updateStatus(
					event.bookingId(),
					BookingStatus.CANCELED,
					"Automatic Cancellation: Refund Processed",
					null,
					null);

		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[booking-service], Error: {}",
					TOPIC_PAYMENT_REFUND, e.getMessage());
		}
	}
}
