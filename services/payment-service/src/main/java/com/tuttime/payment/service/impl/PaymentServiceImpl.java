package com.tuttime.payment.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuttime.common.enums.PaymentStatus;
import com.tuttime.common.event.kafka.EventProducer;
import com.tuttime.common.event.kafka.PaymentEvent;
import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.PaymentAuditLog;
import com.tuttime.payment.entity.Refund;
import com.tuttime.payment.repository.PaymentAuditLogRepository;
import com.tuttime.payment.repository.PaymentRepository;
import com.tuttime.payment.repository.RefundRepository;
import com.tuttime.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.loolzaaa.youkassa.client.ApiClient;
import ru.loolzaaa.youkassa.pojo.Amount;
import ru.loolzaaa.youkassa.pojo.Confirmation;
import ru.loolzaaa.youkassa.processors.PaymentProcessor;
import ru.loolzaaa.youkassa.processors.RefundProcessor;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	@Value("${yookassa.shop-id}")
	private String shopId;

	@Value("${yookassa.secret-key}")
	private String secretKey;

	private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;
	private final PaymentAuditLogRepository paymentAuditLogRepository;
	private final EventProducer eventProducer;
	private final ApiClient apiKassaClient;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional
	public String initiatePaymentLifecycle(UUID bookingId, UUID userId, UUID branchId, BigDecimal amount) {
		// 1. Check for existing PENDING payment
		var existing = paymentRepository.findByBookingId(bookingId);
		if (existing.isPresent() && PaymentStatus.PENDING == existing.get().getStatus()) {
			return existing.get().getConfirmationUrl();
		}

		// 2. SIMULATION CHECK: If no keys, return a mock URL immediately
		// In a real project, you might use a Spring Profile like @Profile("dev")
		if (apiKassaClient == null || "test_placeholder_key_12345".equals(secretKey)) {
			return createMockPayment(bookingId, userId, branchId, amount);
		}

		// Otherwise, proceed with real YooKassa call
		return createNewPayment(bookingId, userId, branchId, amount);
	}

	@Override
	@Transactional
	public void synchronizeTransactionState(String rawPayload) {
		try {
			JsonNode root = objectMapper.readTree(rawPayload);
			String eventId = root.get("id").asText();

			// 1. Idempotency Check (Production critical)
			if (paymentAuditLogRepository.existsByEventId(eventId)) return;

			JsonNode obj = root.get("object");
			String yooId = obj.get("id").asText();
			String yooStatus = obj.get("status").asText().toUpperCase();

			paymentRepository.findByYookassaPaymentId(yooId).ifPresent(payment -> {
				// 2. Update Status mapping
				if ("SUCCEEDED".equals(yooStatus)) {
					payment.setStatus(PaymentStatus.COMPLETED);
				} else if ("CANCELED".equals(yooStatus)) {
					payment.setStatus(PaymentStatus.FAILED);
				}

				// 3. Save DB first (Atomic requirement)
				paymentRepository.save(payment);

				PaymentAuditLog audit = new PaymentAuditLog();
				audit.setPaymentId(payment.getId());
				audit.setEventId(eventId);
				audit.setEventType(root.get("event").asText());
				audit.setEventData(rawPayload);
				paymentAuditLogRepository.save(audit);

				// 4. Trigger Event ONLY if status is COMPLETED
				if (payment.getStatus() == PaymentStatus.COMPLETED) {
					dispatchPaymentCompletedEvent(payment, yooId);
				}

				log.info("Payment {} synchronized to status {}", yooId, payment.getStatus());
			});
		} catch (Exception e) {
			log.error("Webhook synchronization failed", e);
		}
	}

	@Override
	@Transactional
	public void processAdministrativeRefund(UUID paymentId, BigDecimal amount, String reason) {
		Payment payment = paymentRepository.findById(paymentId)
				.orElseThrow(() -> new RuntimeException("Payment not found"));

		RefundProcessor processor = new RefundProcessor(apiKassaClient);
		String idempotencyKey = UUID.randomUUID().toString();

		var yooRefund = processor.create(ru.loolzaaa.youkassa.model.Refund.builder()
				.paymentId(payment.getYookassaPaymentId())
				.amount(new Amount(amount.toString(), "RUB"))
				.build(), idempotencyKey);

		refundRepository.save(Refund.builder()
				.payment(payment)
				.yookassaRefundId(yooRefund.getId())
				.amount(amount)
				.status(yooRefund.getStatus().toUpperCase())
				.reason(reason)
				.build());
	}

	@Override
	public String fetchPaymentStatus(UUID bookingId) {
		return paymentRepository.findByBookingId(bookingId)
				.map(p -> p.getStatus().name())
				.orElse("NOT_FOUND");
	}

	@Override
	@Transactional
	public void testManualPaymentSuccess(UUID bookingId, String mockYooId) {
		Payment payment = paymentRepository.findByBookingId(bookingId)
				.orElseGet(() -> {
					log.info("[TEST] Creating mock payment record for booking: {}", bookingId);
					Payment mock = Payment.builder()
							.bookingId(bookingId)
							.userId(UUID.randomUUID())
							.branchId(UUID.randomUUID())
							.amount(new BigDecimal("100.00"))
							.currency("RUB")
							.status(PaymentStatus.PENDING)
							.build();
					return paymentRepository.save(mock);
				});

		payment.setStatus(PaymentStatus.COMPLETED);
		payment.setYookassaPaymentId(mockYooId);
		payment.setPaymentMethodType("manual_test_card");
		paymentRepository.save(payment);

		dispatchPaymentCompletedEvent(payment, mockYooId);

		log.info("[TEST] Simulated success for booking {}", bookingId);
	}

	private String createNewPayment(UUID bookingId, UUID userId, UUID branchId, BigDecimal amount) {
		PaymentProcessor processor = new PaymentProcessor(apiKassaClient);
		UUID idempotencyKey = UUID.randomUUID();

		var yooRequest = ru.loolzaaa.youkassa.model.Payment.builder()
				.amount(new Amount(amount.toString(), "RUB"))
				.confirmation(Confirmation.builder()
						.type("redirect")
						.returnUrl("tuttime://checkout/complete/" + bookingId)
						.build())
				.capture(true)
				.description("Booking " + bookingId)
				.build();

		var yooResponse = processor.create(yooRequest, idempotencyKey.toString());

		Payment payment = Payment.builder()
				.bookingId(bookingId)
				.userId(userId)
				.branchId(branchId)
				.amount(amount)
				.currency("RUB")
				.yookassaPaymentId(yooResponse.getId())
				.idempotencyKey(idempotencyKey)
				.confirmationUrl(yooResponse.getConfirmation().getConfirmationUrl())
				.status(PaymentStatus.PENDING)
				.build();

		return paymentRepository.save(payment).getConfirmationUrl();
	}

	private String createMockPayment(UUID bookingId, UUID userId, UUID branchId, BigDecimal amount) {
		String mockYooId = "mock_pay_" + UUID.randomUUID();
		String mockUrl = "https://example.com" + mockYooId;

		Payment payment = Payment.builder()
				.bookingId(bookingId)
				.userId(userId)
				.branchId(branchId)
				.amount(amount)
				.currency("RUB")
				.yookassaPaymentId(mockYooId)
				.idempotencyKey(UUID.randomUUID())
				.confirmationUrl(mockUrl)
				.status(PaymentStatus.PENDING)
				.build();

		return paymentRepository.save(payment).getConfirmationUrl();
	}

	/**
	 * Shared helper to ensure Kafka events are consistent between Prod and Test
	 */
	private void dispatchPaymentCompletedEvent(Payment payment, String externalId) {
		PaymentEvent.PaymentCompletedEvent event = PaymentEvent.PaymentCompletedEvent.builder()
				.bookingId(payment.getBookingId())
				.userId(payment.getUserId())
				.branchId(payment.getBranchId())
				.externalPaymentId(externalId)
				.amount(payment.getAmount())
				.currency(payment.getCurrency())
				.commissionRate(BigDecimal.valueOf(0.01))
				.build();
		eventProducer.sendPaymentCompletedEvent(event);
	}
}
