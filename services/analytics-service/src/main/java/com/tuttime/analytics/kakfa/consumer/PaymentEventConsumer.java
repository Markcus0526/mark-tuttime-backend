package com.tuttime.analytics.kakfa.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.analytics.service.AnalyticsService;
import com.tuttime.common.event.PaymentEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import static com.tuttime.analytics.constants.AnalyticsConstants.*;
import static com.tuttime.common.event.EventProducer.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventConsumer {

	private final ObjectMapper objectMapper;
	private final AnalyticsService analyticsService;

	/**
	 * Confirms the booking once payment is successful.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_COMPLETED,
			groupId = "${spring.kafka.consumer.group-id}")
	public void handlePaymentCompleted(String eventString) {
		try {
			if (!StringUtils.hasText(eventString)) return;

			PaymentEvent.PaymentCompletedEvent event = objectMapper.readValue(eventString, PaymentEvent.PaymentCompletedEvent.class);

			log.info("Kafka: Payment confirmed for booking {}", event.bookingId());

			// Mapping the specific event to the generic Analytics log
			analyticsService.processEvent(
					SOURCE_PAYMENT_SERVICE,
					EVENT_PAYMENT_SUCCESS,
					event.branchId(), // Important: Pass the branchId for physical tracking
					event);
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[analytics-service], Error: {}", TOPIC_PAYMENT_COMPLETED, e.getMessage());
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

			PaymentEvent.PaymentRefundedEvent event = objectMapper.readValue(eventString, PaymentEvent.PaymentRefundedEvent.class);

			log.info("Kafka: Refund received for booking {}. Releasing table.", event.bookingId());

			// Mapping the specific event to the generic Analytics log
			analyticsService.processEvent(
					SOURCE_PAYMENT_SERVICE,
					EVENT_PAYMENT_REFUNDED,
					event.branchId(), // Important: Pass the branchId for physical tracking
					event);
		} catch (Exception e) {
			log.error("<<< KAFKA[{}] >>> Consumer:[analytics-service], Error: {}", TOPIC_PAYMENT_REFUND, e.getMessage());
		}
	}
}
