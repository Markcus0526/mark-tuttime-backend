package com.tuttime.payment.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import com.tuttime.payment.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseUuidEntity {

	@Column(name = "booking_id", nullable = false)
	private UUID bookingId;

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "stripe_session_id", unique = true)
	private String stripeSessionId;

	@Column(name = "payment_intent_id", unique = true)
	private String paymentIntentId;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 10)
	@Builder.Default
	private String currency = "USD";

	@Column(name = "payment_method_type", length = 50)
	private String paymentMethodType; // e.g., "card", "apple_pay"

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	@Builder.Default
	private PaymentStatus status = PaymentStatus.PENDING;

	@Column(name = "receipt_url", length = 500)
	private String receiptUrl;

	@Column(name = "failure_message", columnDefinition = "TEXT")
	private String failureMessage;
}
