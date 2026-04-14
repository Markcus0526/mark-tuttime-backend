package com.tuttime.payment.service.impl;

import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.tuttime.common.event.EventProducer;
import com.tuttime.common.event.PaymentEvent;
import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.MethodArgumentNotValidException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.payment.entity.Payment;
import com.tuttime.payment.entity.Refund;
import com.tuttime.payment.entity.enums.PaymentStatus;
import com.tuttime.payment.repository.PaymentRepository;
import com.tuttime.payment.repository.RefundRepository;
import com.tuttime.payment.service.PaymentService;
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
@Transactional(readOnly = true)
@Slf4j
public class PaymentServiceImpl implements PaymentService {

	private final PaymentRepository paymentRepository;
	private final RefundRepository refundRepository;
	private final EventProducer eventProducer;

	@Value("${stripe.webhook.secret}")
	private String webhookSecret;

	@Override
	@Transactional
	public String createCheckoutSession(UUID bookingId, UUID userId, BigDecimal amount) {
		try {
			// 1. Create Stripe Session
			SessionCreateParams params = SessionCreateParams.builder()
					// 1. Payment Methods (Stripe handles Apple Pay automatically if enabled in Dashboard)
					.addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)

					// 2. Mode (Standard one-time payment)
					.setMode(SessionCreateParams.Mode.PAYMENT)

					// 3. Redirect URLs (Where the user goes after paying in the Mobile App/Browser)
					.setSuccessUrl("https://tuttime.com" + bookingId)
					.setCancelUrl("https://tuttime.com")

					// 4. Metadata (CRITICAL: This is how the Webhook knows which booking to confirm)
					.putMetadata("bookingId", bookingId.toString())

					// 5. Line Items (The actual "Product" shown on the Apple Pay screen)
					.addLineItem(SessionCreateParams.LineItem.builder()
							.setQuantity(1L)
							.setPriceData(SessionCreateParams.LineItem.PriceData.builder()
									.setCurrency("usd")
									// Stripe expects amounts in CENTS (e.g., $10.00 = 1000)
									.setUnitAmount(amount.multiply(new BigDecimal(100)).longValue())
									.setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
											.setName("Table Reservation")
											.setDescription("Reservation for Booking #" + bookingId.toString().substring(0, 8))
											.build())
									.build())
							.build())
					.build();

			Session session = Session.create(params);

			// 2. Save PENDING record
			paymentRepository.save(Payment.builder()
					.bookingId(bookingId).userId(userId).amount(amount)
					.stripeSessionId(session.getId()).status(PaymentStatus.PENDING)
					.build());

			return session.getUrl();
		} catch (StripeException e) {
			throw new BusinessException("Stripe error", e);
		}
	}

	@Override
	@Transactional
	public void processRefund(UUID bookingId, BigDecimal amount, String reason) {
		Payment payment = paymentRepository.findByBookingId(bookingId)
				.orElseThrow(() -> new MethodArgumentNotValidException("Payment not found"));

		try {
			// Create the params using our builder
			RefundCreateParams params = buildStripeRefundParams(payment, amount, reason);

			// Execute the call to Stripe
			com.stripe.model.Refund stripeRefund = com.stripe.model.Refund.create(params);

			// Save local record (Audit)
			refundRepository.save(Refund.builder()
					.paymentId(payment.getId())
					.stripeRefundId(stripeRefund.getId())
					.amount(amount)
					.status(stripeRefund.getStatus().toUpperCase()) // e.g., "SUCCEEDED" or "PENDING"
					.reason(reason)
					.build());

			PaymentEvent.PaymentRefundedEvent event = new PaymentEvent.PaymentRefundedEvent(
					bookingId,
					amount,
					OffsetDateTime.now()
			);
			eventProducer.sendPaymentRefundEvent(event);
		} catch (StripeException e) {
			throw new BusinessException("Stripe Refund Error", e);
		}
	}

	/**
	 * Builds the parameters for a Stripe Refund.
	 * Ensures the Booking ID is attached for cross-service tracking.
	 */
	private RefundCreateParams buildStripeRefundParams(Payment payment, BigDecimal refundAmount, String reason) {
		return RefundCreateParams.builder()
				// 1. Link to the original Payment Intent (Required)
				.setPaymentIntent(payment.getPaymentIntentId())

				// 2. Amount in CENTS (Stripe requirement)
				// e.g., $50.00 -> 5000
				.setAmount(refundAmount.multiply(new BigDecimal(100)).longValue())

				// 3. Metadata (CRITICAL for your Webhook/Kafka logic)
				// This allows the 'charge.refunded' webhook to find the right Booking
				.putMetadata("bookingId", payment.getBookingId().toString())
				.putMetadata("reason", reason != null ? reason : "Customer Request")

				// 4. Standard Stripe Reasons (Optional but helps in Stripe Dashboard)
				// Options: DUPLICATE, FRAUDULENT, REQUESTED_BY_CUSTOMER
				.setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)

				// 5. Instruction to refund the full amount (only if you don't set 'amount')
				// .setReverseTransfer(true)
				.build();
	}

}
