package com.tuttime.payment.service.impl;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.PaymentEvent;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.common.enums.PaymentStatus;
import com.tuttime.payment.repository.PaymentAuditLogRepository;
import com.tuttime.payment.repository.PaymentRepository;
import com.tuttime.payment.service.StripeWebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
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
			throw new InvalidRequestException("Invalid signature");
		}

		// 2. IDEMPOTENCY CHECK (Exactly-once processing)
		if (auditLogRepository.existsByStripeEventId(event.getId())) {
			log.info("Event {} already processed. Acknowledging with 200 OK.", event.getId());
			return; // Exit early; Stripe will see this as successful
		}

		// 3. PROCESS SPECIFIC EVENT TYPES
		if ("checkout.session.completed".equals(event.getType())) {
			Session session = (Session) event.getDataObjectDeserializer().getObject()
					.orElseThrow(() -> new InvalidRequestException("Empty event data"));

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
				.orElseThrow(() -> new InvalidRequestException("Payment missing for booking: " + bookingId));

		if (payment.getStatus() != PaymentStatus.COMPLETED) {
			payment.setStatus(PaymentStatus.COMPLETED);
			payment.setPaymentIntentId(session.getPaymentIntent());
			payment.setPaymentMethodType(session.getPaymentMethodTypes().getFirst());
			paymentRepository.save(payment);

			// Trigger internal and external confirmation (e.g. Kafka/Email)
			// Notify Kafka/Events
			// BROADCAST TO KAFKA
			PaymentEvent.PaymentCompletedEvent event = PaymentEvent.PaymentCompletedEvent.builder()
					.bookingId(payment.getBookingId())
					.userId(payment.getUserId())
					.branchId(payment.getBranchId())
					.stripePaymentIntentId(payment.getStripeSessionId())
					.amount(payment.getAmount())
					.commissionRate(BigDecimal.valueOf(0.01))
					.currency(payment.getCurrency())
					.build();
			eventProducer.sendPaymentCompletedEvent(event);

			log.info("Payment SUCCESS confirmed for Booking ID: {}", bookingId);
		}
	}
}
