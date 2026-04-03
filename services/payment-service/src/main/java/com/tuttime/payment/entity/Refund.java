package com.tuttime.payment.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "refunds", schema = "payment_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund extends BaseUuidEntity {

	@Column(name = "payment_id", nullable = false)
	private UUID paymentId;

	@Column(name = "stripe_refund_id", nullable = false, unique = true)
	private String stripeRefundId;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal amount;

	@Column(nullable = false, length = 20)
	private String status; // SUCCEEDED, FAILED

	@Column(length = 255)
	private String reason;
}
