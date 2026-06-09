package com.tuttime.notification.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.NotificationTemplateKey;
import com.tuttime.common.enums.PaymentStatus;
import com.tuttime.common.event.kafka.event.PaymentEvent;
import com.tuttime.notification.service.NotificationOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_PAYMENT_COMPLETED;
import static com.tuttime.common.event.kafka.TopicConstants.TOPIC_PAYMENT_REFUND;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

	private final ObjectMapper objectMapper;
	private final NotificationOrchestrator notificationOrchestrator;

	/**
	 * Handles Payment Success notifications.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_COMPLETED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handlePaymentCompleted(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			// 1. Parse the specific event
			PaymentEvent.PaymentCompletedEvent event =
					objectMapper.readValue(eventString, PaymentEvent.PaymentCompletedEvent.class);

			// Map event data to template parameters
			Map<String, Object> params = Map.of(
					"amount", event.amount(),
					"currency", event.currency(),
					"bookingId", event.bookingId(),
					"transactionId", event.externalPaymentId()
			);

			NotificationTemplateKey templateKey = NotificationTemplateKey.PAYMENT_SUCCESS;
			if (event.status() == PaymentStatus.FAILED || event.status() == PaymentStatus.EXPIRED)
				templateKey = NotificationTemplateKey.PAYMENT_FAILED;

			// 3. Delegate to Notification Service
			// We assume your event contains the userId who should receive the alert
			notificationOrchestrator.handleNotification(
					event.eventId(),
					event.userId(),
					templateKey,
					params);

		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: {}",
					TOPIC_PAYMENT_COMPLETED, e.getMessage());
			throw new RuntimeException("notification-service failed to process " + TOPIC_PAYMENT_COMPLETED, e);
		}
	}

	/**
	 * Handles Refund processed notifications.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_REFUND,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handleRefundProcessed(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			PaymentEvent.PaymentRefundedEvent event =
					objectMapper.readValue(eventString, PaymentEvent.PaymentRefundedEvent.class);

			// 1. Prepare parameters for the "REFUND_PROCESSED" template
			Map<String, Object> params = Map.of(
					"refundAmount", event.amount(),
					"bookingId", event.bookingId(),
					"reason", event.reason() != null ? event.reason() : "Customer Request"
			);

			// 2. Trigger Orchestrator
			notificationOrchestrator.handleNotification(
					event.eventId(),
					event.userId(),
					NotificationTemplateKey.PAYMENT_REFUNDED,
					params);

		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[notification-service], Error: {}",
					TOPIC_PAYMENT_REFUND, e.getMessage());
			throw new RuntimeException("notification-service failed to process " + TOPIC_PAYMENT_REFUND, e);
		}
	}

}
