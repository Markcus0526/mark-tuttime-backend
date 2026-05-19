package com.tuttime.payment.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import com.tuttime.common.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payment_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Payment extends BaseUuidEntity {

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "branch_id", nullable = false)
    private UUID branchId;

    @Column(name = "yookassa_payment_id", unique = true)
    private String yookassaPaymentId;

    @Column(name = "idempotency_key", unique = true)
    private UUID idempotencyKey;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    @Builder.Default
    private String currency = "RUB";

    @Column(name = "payment_method_type", length = 50)
    private String paymentMethodType; // e.g., "card", "apple_pay"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "confirmation_url", length = 500)
    private String confirmationUrl;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;
}
