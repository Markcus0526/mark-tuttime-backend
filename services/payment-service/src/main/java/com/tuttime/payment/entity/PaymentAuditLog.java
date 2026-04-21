package com.tuttime.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "payment_audit_logs", schema = "payment_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PaymentAuditLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "payment_id")
	private UUID paymentId;

	@Column(name = "stripe_event_id", unique = true, nullable = false)
	private String stripeEventId;

	@Column(name = "event_type", nullable = false, length = 100)
	private String eventType;

	@JdbcTypeCode(SqlTypes.JSON) // Hibernate 6 (Spring Boot 3.3) way to handle JSONB
	@Column(name = "event_data", columnDefinition = "jsonb")
	private String eventData;

}
