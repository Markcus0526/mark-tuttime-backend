package com.tuttime.booking.kafka.consumer;

import com.tuttime.booking.entity.enums.BookingStatus;
import com.tuttime.booking.service.BookingService;
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
public class PaymentConsumer {
	private final BookingService bookingService;

	/**
	 * Confirms the booking once payment is successful.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_COMPLETED,
			groupId = "booking-service-group")
	public void handlePaymentCompleted(PaymentEvent.PaymentCompletedEvent event) {
		log.info("Kafka: Payment confirmed for booking {}", event.bookingId());

		// This method should update status to PAID and trigger the 'StatusChangedEvent'
		bookingService.updateStatus(
				event.bookingId(),
				BookingStatus.PAID,
				null, // System Actor
				"Stripe Payment Verified: " + event.stripePaymentIntentId()
		);
	}

	/**
	 * Cancels the booking and frees the table if a refund occurs.
	 */
	@KafkaListener(
			topics = TOPIC_PAYMENT_REFUND,
			groupId = "booking-service-group")
	public void handlePaymentRefunded(PaymentEvent.PaymentRefundedEvent event) {
		log.info("Kafka: Refund received for booking {}. Releasing table.", event.bookingId());

		bookingService.updateStatus(
				event.bookingId(),
				BookingStatus.CANCELED,
				null,
				"Automatic Cancellation: Refund Processed"
		);
	}
}
