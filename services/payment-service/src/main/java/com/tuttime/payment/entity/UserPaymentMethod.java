package com.tuttime.payment.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "user_payment_methods", indexes = {
		@Index(name = "idx_payment_methods_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPaymentMethod extends BaseUuidEntity {

	@Column(name = "user_id", nullable = false)
	private UUID userId;

	@Column(name = "yookassa_payment_method_id", nullable = false, unique = true)
	private String yookassaPaymentMethodId;

	@Column(name = "card_type", length = 50)
	private String cardType; // e.g., Visa, MasterCard, MIR

	@Column(name = "last4", length = 4)
	private String last4;

	@Column(name = "expiry_month", length = 2)
	private String expiryMonth;

	@Column(name = "expiry_year", length = 4)
	private String expiryYear;

	@Builder.Default
	@Column(name = "default_method", nullable = false)
	private Boolean defaultMethod = false;

	@Builder.Default
	@Column(name = "active", nullable = false)
	private Boolean active = true;
}

