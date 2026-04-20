package com.tuttime.payment.kafka;

import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.PaymentEvent;
import com.tuttime.payment.entity.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

	private final EventProducer eventProducer;

	public void sendPaymentSuccess(Payment payment) {
		PaymentEvent.PaymentCompletedEvent event = new PaymentEvent.PaymentCompletedEvent(
				payment.getBookingId(),
				payment.getUserId(),
				payment.getBranchId(),
				payment.getStripeSessionId(),
				payment.getAmount(),
				payment.getCurrency(),
				OffsetDateTime.now()
		);

		log.info("Publishing payment success for booking: {}", event.bookingId());

		// Use bookingId as the partition key to ensure order of events
		eventProducer.sendPaymentCompletedEvent(event);
	}
}