package com.tuttime.payment.service.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.PaymentEvent;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.payment.entity.enums.PaymentStatus;
import com.tuttime.payment.repository.PaymentAuditLogRepository;
import com.tuttime.payment.repository.PaymentRepository;
import com.tuttime.payment.service.StripeWebhookService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookServiceImpl implements StripeWebhookService {

	private final PaymentRepository paymentRepository;
	private final PaymentAuditLogRepository auditLogRepository;
	private final EventProducer eventProducer;

	@Value("${stripe.webhook.secret}")
	private String webhookSecret; // Found in Stripe Dashboard -> Developers -> Webhooks

	@Override
	@Transactional
	public void handleWebhook(String payload, String sigHeader) {
		Event event;
		try {
			// 1. VERIFY SIGNATURE (Security)
			event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
		} catch (SignatureVerificationException e) {
			log.error("Stripe signature verification failed: {}", e.getMessage());
			throw new IllegalArgumentException("Invalid signature");
		}

		// 2. IDEMPOTENCY CHECK (Exactly-once processing)
		if (auditLogRepository.existsByStripeEventId(event.getId())) {
			log.info("Event {} already processed. Acknowledging with 200 OK.", event.getId());
			return; // Exit early; Stripe will see this as successful
		}

		// 3. PROCESS SPECIFIC EVENT TYPES
		if ("checkout.session.completed".equals(event.getType())) {
			Session session = (Session) event.getDataObjectDeserializer().getObject()
					.orElseThrow(() -> new IllegalStateException("Empty event data"));

			processCheckoutCompleted(session);
		}

		// 4. LOG AUDIT TO PREVENT DUPLICATES
		auditLogRepository.save(PaymentAuditLog.builder()
				.stripeEventId(event.getId())
				.eventType(event.getType())
				.eventData(payload)
				.build());
	}

	private void processCheckoutCompleted(Session session) {
		// Metadata was set during session creation in createCheckoutSession
		UUID bookingId = UUID.fromString(session.getMetadata().get("bookingId"));

		Payment payment = paymentRepository.findByBookingId(bookingId)
				.orElseThrow(() -> new ResourceNotFoundException("Payment missing for booking: " + bookingId));

		if (payment.getStatus() != PaymentStatus.COMPLETED) {
			payment.setStatus(PaymentStatus.COMPLETED);
			payment.setPaymentIntentId(session.getPaymentIntent());
			payment.setPaymentMethodType(session.getPaymentMethodTypes().get(0));
			paymentRepository.save(payment);

			// Trigger internal and external confirmation (e.g. Kafka/Email)
			// Notify Kafka/Events
			// BROADCAST TO KAFKA
			PaymentEvent.PaymentCompletedEvent event = new PaymentEvent.PaymentCompletedEvent(
					payment.getBookingId(),
					payment.getUserId(),
					session.getPaymentIntent(),
					payment.getAmount(),
					payment.getCurrency(),
					OffsetDateTime.now()
			);
			eventProducer.publishPaymentCompletedEvent(event);

			log.info("Payment SUCCESS confirmed for Booking ID: {}", bookingId);
		}
	}
}
